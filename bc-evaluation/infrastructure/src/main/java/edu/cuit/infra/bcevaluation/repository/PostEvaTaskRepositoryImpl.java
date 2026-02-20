package edu.cuit.infra.bcevaluation.repository;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.CourseIdsByTeacherIdQueryPort;
import edu.cuit.bc.course.application.port.CourseTeacherAndSemesterQueryPort;
import edu.cuit.bc.course.application.port.CourInfTimeSlotQueryPort;
import edu.cuit.bc.course.application.port.SemesterStartDateQueryPort;
import edu.cuit.bc.evaluation.application.model.PostEvaTaskCommand;
import edu.cuit.bc.evaluation.application.port.PostEvaTaskRepository;
import edu.cuit.bc.evaluation.domain.PostEvaTaskQueryException;
import edu.cuit.bc.evaluation.domain.PostEvaTaskUpdateException;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.infra.gateway.impl.eva.util.CalculateClassTime;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 发布评教任务端口适配器（保持历史行为不变：原样搬运旧 gateway 写流程）。
 */
@Component
@RequiredArgsConstructor
public class PostEvaTaskRepositoryImpl implements PostEvaTaskRepository {
    private final EvaTaskMapper evaTaskMapper;
    private final CourInfTimeSlotQueryPort courInfTimeSlotQueryPort;
    private final CourseIdsByTeacherIdQueryPort courseIdsByTeacherIdQueryPort;
    private final CourseTeacherAndSemesterQueryPort courseTeacherAndSemesterQueryPort;
    private final SemesterStartDateQueryPort semesterStartDateQueryPort;
    @Autowired
    @Qualifier("sysUserMapper")
    private Object sysUserMapper;
    private final EvaCacheConstants evaCacheConstants;
    private final LocalCacheManager localCacheManager;

    @Override
    @Transactional
    public Integer create(PostEvaTaskCommand command, Integer maxBeEvaNum) {
        // 同时发送该任务的评教待办消息（已迁移为：提交后事件触发；此处仅负责写侧主流程）
        CourInfTimeSlotQueryPort.CourInfTimeSlot courInfDO = courInfTimeSlotQueryPort.findByCourInfId(command.courInfId()).orElse(null);
        if (courInfDO == null) {
            throw new PostEvaTaskUpdateException("并没有找到相关课程");
        }
        CourseTeacherAndSemesterQueryPort.CourseTeacherAndSemester course =
                courseTeacherAndSemesterQueryPort.findByCourseId(courInfDO.courseId()).orElse(null);
        // 选中的课程是否已经上完
        LocalDate semesterStartDate = semesterStartDateQueryPort.findStartDateBySemesterId(course.semesterId()).orElse(null);
        LocalDate localDate = semesterStartDate.plusDays((courInfDO.week() - 1) * 7L + courInfDO.day() - 1);

        Integer f = 2;// 判断是不是课程快已经结束 1冲0无
        if (localDate.getYear() >= LocalDate.now().getYear()) {
            if (localDate.getYear() == LocalDate.now().getYear()) {
                if (localDate.getMonthValue() < LocalDate.now().getMonthValue()) {
                    f = 1;
                } else if (localDate.getMonthValue() > LocalDate.now().getMonthValue()) {
                    f = 0;
                } else {
                    if (localDate.getDayOfMonth() > LocalDate.now().getDayOfMonth()) {
                        f = 0;
                    } else {
                        if (localDate.getDayOfMonth() == LocalDate.now().getDayOfMonth()) {
                            String dateTime = localDate + " 00:00";// 因为少了一个空格而不能满足格式而报错
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                            LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);
                            if (CalculateClassTime.calculateClassTime(localDateTime, courInfDO.startTime()).isBefore(LocalDateTime.now())) {
                                f = 1;
                            } else {
                                f = 0;
                            }
                        }
                        if (localDate.getDayOfMonth() < LocalDate.now().getDayOfMonth()) {
                            f = 1;
                        }
                    }
                }
            } else {
                f = 0;
            }
        } else {
            f = 1;
        }
        if (f == 1) {
            throw new PostEvaTaskUpdateException("课程已经开始了哦");
        }
        // 看看是否和老师自己的课有冲突
        List<Integer> courseIds = courseIdsByTeacherIdQueryPort.findCourseIdsByTeacherId(command.evaluatorId());
        if (CollectionUtil.isNotEmpty(courseIds)) {
            List<CourInfTimeSlotQueryPort.CourInfTimeSlot> courInfDOList = courInfTimeSlotQueryPort.findByCourseIds(courseIds);
            for (int i = 0; i < courInfDOList.size(); i++) {
                if (courInfDO.week().equals(courInfDOList.get(i).week())) {
                    if (courInfDO.day().equals(courInfDOList.get(i).day())) {
                        if (((courInfDO.startTime() <= courInfDOList.get(i).endTime()) && (courInfDO.endTime() >= courInfDOList.get(i).startTime()))
                                || ((courInfDOList.get(i).startTime() <= courInfDO.endTime()) && (courInfDOList.get(i).endTime() >= courInfDO.startTime()))) {
                            throw new PostEvaTaskUpdateException("与你其他课程冲突");
                        }
                    }
                }
            }
        }
        // 判定是否超过最大评教次数
        Object teacher = selectSysUserById(course.teacherId());
        List<Integer> evaCourseIds = courseIdsByTeacherIdQueryPort.findCourseIdsByTeacherId(selectSysUserId(teacher));
        if (CollectionUtil.isNotEmpty(evaCourseIds)) {
            List<CourInfTimeSlotQueryPort.CourInfTimeSlot> evaCourInfoDOs = courInfTimeSlotQueryPort.findByCourseIds(evaCourseIds);
            if (CollectionUtil.isNotEmpty(evaCourInfoDOs)) {
                List<Integer> evaCourInfoIds = evaCourInfoDOs.stream().map(CourInfTimeSlotQueryPort.CourInfTimeSlot::id).toList();
                List<EvaTaskDO> evaTaskDOList1 = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>()
                        .in("cour_inf_id", evaCourInfoIds)
                        .eq("status", 0)
                        .or()
                        .eq("status", 1)
                        .in("cour_inf_id", evaCourInfoIds));
                if (evaTaskDOList1.size() >= maxBeEvaNum) {
                    throw new PostEvaTaskQueryException("任务发起失败，该老师本学期的被评教次数已达上限，不可再进行评教！");
                }
            }
        }

        // 看看是否和老师其他评教任务有冲突
        List<EvaTaskDO> evaTaskDOList = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id", command.evaluatorId()).eq("status", 0));
        List<Integer> courInfoIds = evaTaskDOList.stream().map(EvaTaskDO::getCourInfId).toList();
        if (CollectionUtil.isNotEmpty(courInfoIds)) {
            List<CourInfTimeSlotQueryPort.CourInfTimeSlot> evaCourInfDOList = courInfTimeSlotQueryPort.findByCourInfIds(courInfoIds);
            for (int i = 0; i < evaCourInfDOList.size(); i++) {
                if (courInfDO.week().equals(evaCourInfDOList.get(i).week())) {
                    if (courInfDO.day().equals(evaCourInfDOList.get(i).day())) {
                        if ((courInfDO.startTime() <= evaCourInfDOList.get(i).endTime() && courInfDO.endTime() >= evaCourInfDOList.get(i).startTime())
                                || (evaCourInfDOList.get(i).startTime() <= courInfDO.endTime() && evaCourInfDOList.get(i).endTime() >= courInfDO.startTime())) {
                            throw new PostEvaTaskUpdateException("与你其他任务所上课程冲突");
                        }
                    }
                }
            }
        }

        EvaTaskDO evaTaskDO = new EvaTaskDO();
        evaTaskDO.setCreateTime(LocalDateTime.now());
        evaTaskDO.setUpdateTime(LocalDateTime.now());
        evaTaskDO.setStatus(0);
        evaTaskDO.setCourInfId(command.courInfId());
        evaTaskDO.setTeacherId(command.evaluatorId());
        evaTaskMapper.insert(evaTaskDO);
        // 加缓存
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(course.semesterId()));
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_TEACH, selectSysUserNameById(evaTaskDO.getTeacherId()));
        Integer taskId = evaTaskMapper.selectOne(new QueryWrapper<EvaTaskDO>()
                        .eq("teacher_id", command.evaluatorId())
                        .eq("cour_inf_id", command.courInfId())
                        .eq("status", 0))
                .getId();

        if (taskId == null) {
            throw new PostEvaTaskQueryException("没有找到你的id");
        }
        return taskId;
    }

    private Object selectSysUserById(Serializable userId) {
        try {
            Method selectById = sysUserMapper.getClass().getMethod("selectById", Serializable.class);
            return selectById.invoke(sysUserMapper, userId);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (targetException instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(targetException);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private Integer selectSysUserId(Object sysUser) {
        try {
            Method getId = sysUser.getClass().getMethod("getId");
            return (Integer) getId.invoke(sysUser);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (targetException instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(targetException);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private String selectSysUserNameById(Serializable userId) {
        Object sysUser = selectSysUserById(userId);
        try {
            Method getName = sysUser.getClass().getMethod("getName");
            return (String) getName.invoke(sysUser);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (targetException instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(targetException);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
