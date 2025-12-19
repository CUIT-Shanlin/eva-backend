package edu.cuit.infra.bcevaluation.repository;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.evaluation.application.model.PostEvaTaskCommand;
import edu.cuit.bc.evaluation.application.port.PostEvaTaskRepository;
import edu.cuit.bc.evaluation.domain.PostEvaTaskQueryException;
import edu.cuit.bc.evaluation.domain.PostEvaTaskUpdateException;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SemesterDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SemesterMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.infra.gateway.impl.eva.util.CalculateClassTime;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final SemesterMapper semesterMapper;
    private final SysUserMapper sysUserMapper;
    private final EvaCacheConstants evaCacheConstants;
    private final LocalCacheManager localCacheManager;

    @Override
    @Transactional
    public Integer create(PostEvaTaskCommand command, Integer maxBeEvaNum) {
        // 同时发送该任务的评教待办消息（已迁移为：提交后事件触发；此处仅负责写侧主流程）
        CourInfDO courInfDO = courInfMapper.selectById(command.courInfId());
        if (courInfDO == null) {
            throw new PostEvaTaskUpdateException("并没有找到相关课程");
        }
        CourseDO courseDO = courseMapper.selectById(courInfDO.getCourseId());
        // 选中的课程是否已经上完
        SemesterDO semesterDO = semesterMapper.selectById(courseDO.getSemesterId());
        LocalDate localDate = semesterDO.getStartDate().plusDays((courInfDO.getWeek() - 1) * 7L + courInfDO.getDay() - 1);

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
                            if (CalculateClassTime.calculateClassTime(localDateTime, courInfDO.getStartTime()).isBefore(LocalDateTime.now())) {
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
        List<CourseDO> courseDOList = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", command.evaluatorId()));
        List<Integer> courseIds = courseDOList.stream().map(CourseDO::getId).toList();
        if (CollectionUtil.isNotEmpty(courseIds)) {
            List<CourInfDO> courInfDOList = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id", courseIds));
            for (int i = 0; i < courInfDOList.size(); i++) {
                if (courInfDO.getWeek().equals(courInfDOList.get(i).getWeek())) {
                    if (courInfDO.getDay().equals(courInfDOList.get(i).getDay())) {
                        if (((courInfDO.getStartTime() <= courInfDOList.get(i).getEndTime()) && (courInfDO.getEndTime() >= courInfDOList.get(i).getStartTime()))
                                || ((courInfDOList.get(i).getStartTime() <= courInfDO.getEndTime()) && (courInfDOList.get(i).getEndTime() >= courInfDO.getStartTime()))) {
                            throw new PostEvaTaskUpdateException("与你其他课程冲突");
                        }
                    }
                }
            }
        }
        // 判定是否超过最大评教次数
        SysUserDO teacher = sysUserMapper.selectById(courseDO.getTeacherId());
        List<CourseDO> evaCourseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", teacher.getId()));
        List<Integer> evaCourseIds = evaCourseDOS.stream().map(CourseDO::getId).toList();
        if (CollectionUtil.isNotEmpty(evaCourseIds)) {
            List<CourInfDO> evaCourInfoDOs = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id", evaCourseIds));
            if (CollectionUtil.isNotEmpty(evaCourInfoDOs)) {
                List<Integer> evaCourInfoIds = evaCourInfoDOs.stream().map(CourInfDO::getId).toList();
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
            List<CourInfDO> evaCourInfDOList = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("id", courInfoIds));
            for (int i = 0; i < evaCourInfDOList.size(); i++) {
                if (courInfDO.getWeek().equals(evaCourInfDOList.get(i).getWeek())) {
                    if (courInfDO.getDay().equals(evaCourInfDOList.get(i).getDay())) {
                        if ((courInfDO.getStartTime() <= evaCourInfDOList.get(i).getEndTime() && courInfDO.getEndTime() >= evaCourInfDOList.get(i).getStartTime())
                                || (evaCourInfDOList.get(i).getStartTime() <= courInfDO.getEndTime() && evaCourInfDOList.get(i).getEndTime() >= courInfDO.getStartTime())) {
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
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(courseDO.getSemesterId()));
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_TEACH, sysUserMapper.selectById(evaTaskDO.getTeacherId()).getName());
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
}

