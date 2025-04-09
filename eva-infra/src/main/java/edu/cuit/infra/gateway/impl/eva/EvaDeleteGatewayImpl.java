package edu.cuit.infra.gateway.impl.eva;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import edu.cuit.domain.gateway.eva.EvaDeleteGateway;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EvaDeleteGatewayImpl implements EvaDeleteGateway {
    private final FormRecordMapper formRecordMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final FormTemplateMapper formTemplateMapper;
    private final CourOneEvaTemplateMapper courOneEvaTemplateMapper;
    private final CourseMapper courseMapper;
    private final CourInfMapper courInfMapper;
    private final SysUserMapper sysUserMapper;
    private final EvaCacheConstants evaCacheConstants;
    private final LocalCacheManager localCacheManager;
    @Override
    @Transactional
    public Void deleteEvaRecord(List<Integer> ids) {

        for (Integer id : ids) {
            QueryWrapper<FormRecordDO> formRecordWrapper = new QueryWrapper<>();
            formRecordWrapper.eq("id", id);
            if(formRecordMapper.selectOne(formRecordWrapper)==null){
                throw new QueryException("并未找到找到相应评教记录");
            }else {
                FormRecordDO formRecordDO=formRecordMapper.selectById(id);
                if(evaTaskMapper.selectById(formRecordMapper.selectById(id).getTaskId())==null){
                    throw new QueryException("并未找到找到相应评教任务");
                }
                if(courInfMapper.selectById(evaTaskMapper.selectById(formRecordMapper.selectById(id).getTaskId()).getCourInfId()).getCourseId()==null){
                    throw new QueryException("并未找到找到相应课程信息");
                }
                CourseDO courseDO=courseMapper.selectById(courInfMapper.selectById(evaTaskMapper.selectById(formRecordMapper.selectById(id).getTaskId()).getCourInfId()).getCourseId());
                if(courseDO==null){
                    throw new QueryException("没有找到相关课程");
                }
                LogUtils.logContent(sysUserMapper.selectById(evaTaskMapper.selectById(formRecordDO.getTaskId()).getTeacherId()).getName()
                        +" 用户评教任务ID为"+formRecordDO.getTaskId()+"的评教记录");
                formRecordMapper.delete(formRecordWrapper);
                // TODO 并将相关的任务变成未完成
                EvaTaskDO evaTaskDO=evaTaskMapper.selectById(formRecordMapper.selectById(id).getTaskId());
                evaTaskDO.setStatus(0);
                evaTaskMapper.update(evaTaskDO,new QueryWrapper<EvaTaskDO>().eq("id",evaTaskDO.getId()));


                //看看相关课程有没有泡脚记录
                Integer f=0;//0是没有，1是有
                List<CourInfDO> courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id",courseDO.getId()));
                List<Integer> courInfIds=courInfDOS.stream().map(CourInfDO::getId).toList();
                if(CollectionUtil.isEmpty(courInfIds)){
                    throw new UpdateException("该课程下未找到任何课程详情信息");
                }
                List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id",courInfIds));
                List<Integer> evaTaskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
                if(CollectionUtil.isEmpty(evaTaskIds)){
                    f=0;
                }else {
                    List<FormRecordDO> formRecordDOS =formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIds));
                    if(CollectionUtil.isEmpty(formRecordDOS)){
                        f=0;
                    }else {
                        f=1;
                    }
                }
                if(f==0){
                    if(CollectionUtil.isNotEmpty(courOneEvaTemplateMapper.selectList(new QueryWrapper<CourOneEvaTemplateDO>().eq("course_id",courseDO.getId())))) {
                        courOneEvaTemplateMapper.delete(new QueryWrapper<CourOneEvaTemplateDO>().eq("course_id", courseDO.getId()));
                    }
                }
                //删除缓存
                localCacheManager.invalidateCache(evaCacheConstants.ONE_LOG, String.valueOf(id));
                localCacheManager.invalidateCache(null,evaCacheConstants.LOG_LIST);
            }
        }
        return null;
    }
    //ok
    @Override
    @Transactional
    public Void deleteEvaTemplate(List<Integer> ids) {
        for(Integer id : ids){
            //是否是默认数据
            FormTemplateDO formTemplateDO=formTemplateMapper.selectById(id);
            if(formTemplateDO.getIsDefault()==1||formTemplateDO.getIsDefault()==0){
                throw new UpdateException("默认数据不允许删除");
            }
            //没有分配在课程中
            QueryWrapper<CourseDO> courWrapper =new QueryWrapper<>();
            courWrapper.eq("templateId",id);
            CourseDO courseDO=courseMapper.selectOne(courWrapper);
            //获取对应课程id
            if(courseDO==null){
                QueryWrapper<FormTemplateDO> formTemplateWrapper = new QueryWrapper<>();
                formTemplateWrapper.eq("id", id);
                if(formTemplateMapper.selectOne(formTemplateWrapper)==null){
                    throw new QueryException("并未找到找到相应模板");
                }else{
                    //删除模板
                    LogUtils.logContent(formTemplateMapper.selectById(id).getName() +" 评教模板");
                    formTemplateMapper.delete(formTemplateWrapper);
                    //删除缓存
                    localCacheManager.invalidateCache(evaCacheConstants.ONE_TEMPLATE, String.valueOf(id));
                    localCacheManager.invalidateCache(null,evaCacheConstants.TEMPLATE_LIST);
                }
            }else{
                throw new UpdateException("该模板已经被课程分配，无法再进行删除");
            }
        }
        return null;
    }

    @Override
    @Transactional
    public List<Integer> deleteAllTaskByTea(Integer teacherId) {
        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id",teacherId));
        List<Integer> evaTaskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
        if(CollectionUtil.isEmpty(evaTaskDOS)){
            return List.of();
        }
        evaTaskMapper.delete(new QueryWrapper<EvaTaskDO>().eq("teacher_id",teacherId));
        return evaTaskIds;
    }
}
