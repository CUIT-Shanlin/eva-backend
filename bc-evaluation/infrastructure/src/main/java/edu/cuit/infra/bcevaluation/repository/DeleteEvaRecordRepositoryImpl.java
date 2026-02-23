package edu.cuit.infra.bcevaluation.repository;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.CourInfIdsByCourseIdsQueryPort;
import edu.cuit.bc.course.application.port.CourInfTimeSlotQueryPort;
import edu.cuit.bc.course.application.port.CourseTeacherAndSemesterQueryPort;
import edu.cuit.bc.evaluation.application.port.DeleteEvaRecordRepository;
import edu.cuit.bc.evaluation.domain.DeleteEvaRecordQueryException;
import edu.cuit.bc.evaluation.domain.DeleteEvaRecordUpdateException;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    private final CourseTeacherAndSemesterQueryPort courseTeacherAndSemesterQueryPort;
    private final CourInfTimeSlotQueryPort courInfTimeSlotQueryPort;
    private final CourInfIdsByCourseIdsQueryPort courInfIdsByCourseIdsQueryPort;
    @Autowired
    @Qualifier("sysUserMapper")
    private Object sysUserMapper;
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
                if (courInfTimeSlotQueryPort.findByCourInfId(
                        evaTaskMapper.selectById(formRecordMapper.selectById(id).getTaskId()).getCourInfId()
                ).orElse(null).courseId() == null) {
                    throw new DeleteEvaRecordQueryException("并未找到找到相应课程信息");
                }
                Integer courseId = courInfTimeSlotQueryPort.findByCourInfId(
                        evaTaskMapper.selectById(formRecordMapper.selectById(id).getTaskId()).getCourInfId()
                ).orElse(null).courseId();
                if (courseTeacherAndSemesterQueryPort.findByCourseId(courseId).orElse(null) == null) {
                    throw new DeleteEvaRecordQueryException("没有找到相关课程");
                }
                LogUtils.logContent(selectSysUserNameById(evaTaskMapper.selectById(formRecordDO.getTaskId()).getTeacherId())
                        + " 用户评教任务ID为" + formRecordDO.getTaskId() + "的评教记录");
                formRecordMapper.delete(formRecordWrapper);
                deleteCourseTemplateIfNoEvaRecordExists(courseId);
                invalidateEvaRecordCaches(id);
            }
        }
    }

    private void deleteCourseTemplateIfNoEvaRecordExists(Integer courseId) {
        //看看相关课程有没有泡脚记录
        Integer f = 0;//0是没有，1是有
        List<Integer> courInfIds = courInfIdsByCourseIdsQueryPort.findCourInfIdsByCourseIds(List.of(courseId));
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
            if (CollectionUtil.isNotEmpty(courOneEvaTemplateMapper.selectList(new QueryWrapper<CourOneEvaTemplateDO>().eq("course_id", courseId)))) {
                courOneEvaTemplateMapper.delete(new QueryWrapper<CourOneEvaTemplateDO>().eq("course_id", courseId));
            }
        }
    }

    private void invalidateEvaRecordCaches(Integer id) {
        //删除缓存
        localCacheManager.invalidateCache(evaCacheConstants.ONE_LOG, String.valueOf(id));
        localCacheManager.invalidateCache(null, evaCacheConstants.LOG_LIST);
    }

    private static <T> T rethrowInvocationTargetException(InvocationTargetException e) {
        Throwable targetException = e.getTargetException();
        if (targetException instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (targetException instanceof Error error) {
            throw error;
        }
        throw new RuntimeException(targetException);
    }

    private String selectSysUserNameById(Serializable userId) {
        try {
            Method selectById = sysUserMapper.getClass().getMethod("selectById", Serializable.class);
            Object sysUser = selectById.invoke(sysUserMapper, userId);
            Method getName = sysUser.getClass().getMethod("getName");
            return (String) getName.invoke(sysUser);
        } catch (InvocationTargetException e) {
            return rethrowInvocationTargetException(e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
