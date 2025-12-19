package edu.cuit.infra.bcevaluation.repository;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.evaluation.application.port.DeleteEvaRecordRepository;
import edu.cuit.bc.evaluation.domain.DeleteEvaRecordQueryException;
import edu.cuit.bc.evaluation.domain.DeleteEvaRecordUpdateException;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 删除评教记录端口适配器（保持历史行为不变：原样搬运旧 gateway 写流程）。
 */
@Component
@RequiredArgsConstructor
public class DeleteEvaRecordRepositoryImpl implements DeleteEvaRecordRepository {
    private final FormRecordMapper formRecordMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final CourOneEvaTemplateMapper courOneEvaTemplateMapper;
    private final CourseMapper courseMapper;
    private final CourInfMapper courInfMapper;
    private final SysUserMapper sysUserMapper;
    private final EvaCacheConstants evaCacheConstants;
    private final LocalCacheManager localCacheManager;

    @Override
    @Transactional
    public void delete(List<Integer> ids) {
        for (Integer id : ids) {
            QueryWrapper<FormRecordDO> formRecordWrapper = new QueryWrapper<>();
            formRecordWrapper.eq("id", id);
            if (formRecordMapper.selectOne(formRecordWrapper) == null) {
                throw new DeleteEvaRecordQueryException("并未找到找到相应评教记录");
            } else {
                FormRecordDO formRecordDO = formRecordMapper.selectById(id);
                if (evaTaskMapper.selectById(formRecordMapper.selectById(id).getTaskId()) == null) {
                    throw new DeleteEvaRecordQueryException("并未找到找到相应评教任务");
                }
                if (courInfMapper.selectById(evaTaskMapper.selectById(formRecordMapper.selectById(id).getTaskId()).getCourInfId()).getCourseId() == null) {
                    throw new DeleteEvaRecordQueryException("并未找到找到相应课程信息");
                }
                CourseDO courseDO = courseMapper.selectById(courInfMapper.selectById(evaTaskMapper.selectById(formRecordMapper.selectById(id).getTaskId()).getCourInfId()).getCourseId());
                if (courseDO == null) {
                    throw new DeleteEvaRecordQueryException("没有找到相关课程");
                }
                LogUtils.logContent(sysUserMapper.selectById(evaTaskMapper.selectById(formRecordDO.getTaskId()).getTeacherId()).getName()
                        + " 用户评教任务ID为" + formRecordDO.getTaskId() + "的评教记录");
                formRecordMapper.delete(formRecordWrapper);

                //看看相关课程有没有泡脚记录
                Integer f = 0;//0是没有，1是有
                List<CourInfDO> courInfDOS = courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id", courseDO.getId()));
                List<Integer> courInfIds = courInfDOS.stream().map(CourInfDO::getId).toList();
                if (CollectionUtil.isEmpty(courInfIds)) {
                    throw new DeleteEvaRecordUpdateException("该课程下未找到任何课程详情信息");
                }
                List<EvaTaskDO> evaTaskDOS = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id", courInfIds));
                List<Integer> evaTaskIds = evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
                if (CollectionUtil.isEmpty(evaTaskIds)) {
                    f = 0;
                } else {
                    List<FormRecordDO> formRecordDOS = formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id", evaTaskIds));
                    if (CollectionUtil.isEmpty(formRecordDOS)) {
                        f = 0;
                    } else {
                        f = 1;
                    }
                }
                if (f == 0) {
                    if (CollectionUtil.isNotEmpty(courOneEvaTemplateMapper.selectList(new QueryWrapper<CourOneEvaTemplateDO>().eq("course_id", courseDO.getId())))) {
                        courOneEvaTemplateMapper.delete(new QueryWrapper<CourOneEvaTemplateDO>().eq("course_id", courseDO.getId()));
                    }
                }
                //删除缓存
                localCacheManager.invalidateCache(evaCacheConstants.ONE_LOG, String.valueOf(id));
                localCacheManager.invalidateCache(null, evaCacheConstants.LOG_LIST);
            }
        }
    }
}

