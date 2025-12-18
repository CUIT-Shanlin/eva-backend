package edu.cuit.infra.bccourse.adapter;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.UpdateSelfCourseRepository;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeInfoCO;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseTypeCourseDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseTypeDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseTypeCourseMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseTypeMapper;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.enums.cache.ClassroomCacheConstants;
import edu.cuit.infra.enums.cache.CourseCacheConstants;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * bc-course：教师自助改课端口适配器（复用现有表结构与规则，行为保持不变）。
 */
@Component
@RequiredArgsConstructor
public class UpdateSelfCourseRepositoryImpl implements UpdateSelfCourseRepository {
    private final SysUserMapper userMapper;
    private final CourseMapper courseMapper;
    private final SubjectMapper subjectMapper;
    private final CourseTypeCourseMapper courseTypeCourseMapper;
    private final CourseTypeMapper courseTypeMapper;
    private final CourInfMapper courInfMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final LocalCacheManager localCacheManager;
    private final ClassroomCacheConstants classroomCacheConstants;
    private final CourseCacheConstants courseCacheConstants;
    private final EvaCacheConstants evaCacheConstants;

    @Override
    @Transactional
    public Map<String, Map<Integer, Integer>> update(String userName, SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeInfoCO> timeList) {
        String msg = null;
        SysUserDO userDO = userMapper.selectOne(new QueryWrapper<SysUserDO>().eq("username", userName));
        if (userDO == null) {
            throw new QueryException("用户不存在");
        }
        Integer userId = userDO.getId();
        CourseDO courseDO = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", selfTeachCourseCO.getId()).eq("teacher_id", userId));
        if (courseDO == null) {
            // 课程不存在(抛出异常)
            throw new QueryException("用户对应课程不存在");
        }
        List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", userId).eq("semester_id", courseDO.getSemesterId()));
        courseDOS.removeIf(aDo -> aDo.getId().equals(selfTeachCourseCO.getId()));
        SubjectDO subjectDO = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", courseDO.getSubjectId()));
        if (subjectDO == null) {
            throw new QueryException("该课程对应的科目不存在");
        }
        msg = toJudge(courseDO, subjectDO, selfTeachCourseCO);
        // 课程类型
        msg += JudgeCourseType(userDO.getName() + "老师的" + selfTeachCourseCO.getName(), courseDO, selfTeachCourseCO);
        // 课程时间段
        Map<Integer, Integer> taskMap = new HashMap<>();
        String msgEva = "";
        msgEva = JudgeCourseTime(courseDO, timeList, courseDOS, selfTeachCourseCO, taskMap);
        if (!msgEva.isEmpty()) {
            msg += userDO.getName() + "老师的" + selfTeachCourseCO.getName() + "课程的上课时间（教室）被修改了。";
        }
        Map<String, Map<Integer, Integer>> map = new HashMap<>();
        map.put(msg, null);
        map.put(msgEva, taskMap);
        localCacheManager.invalidateCache(null, classroomCacheConstants.ALL_CLASSROOM);
        return map;
    }

    private String JudgeCourseTime(
            CourseDO courseDO,
            List<SelfTeachCourseTimeInfoCO> timeList,
            List<CourseDO> courseDOList,
            SelfTeachCourseCO selfTeachCourseCO,
            Map<Integer, Integer> taskMap
    ) {
        String msg = "";
        List<Integer> courInfoIds = evaTaskMapper.selectList(
                        new QueryWrapper<EvaTaskDO>().eq("teacher_id", courseDO.getTeacherId()).eq("status", 0)
                ).stream()
                .map(EvaTaskDO::getCourInfId)
                .toList();
        List<CourInfDO> courInfoList = courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id", courseDO.getId()));
        SysUserDO userDO = userMapper.selectById(courseDO.getTeacherId());
        List<CourInfDO> courseChangeList = new ArrayList<>();
        for (SelfTeachCourseTimeInfoCO selfTeachCourseTimeCO : timeList) {
            for (Integer week : selfTeachCourseTimeCO.getWeeks()) {
                CourInfDO courInfDO = new CourInfDO();
                courInfDO.setCourseId(courseDO.getId());
                courInfDO.setWeek(week);
                courInfDO.setDay(selfTeachCourseTimeCO.getDay());
                courInfDO.setStartTime(selfTeachCourseTimeCO.getStartTime());
                courInfDO.setEndTime(selfTeachCourseTimeCO.getEndTime());
//               courInfDO.setLocation(selfTeachCourseTimeCO.getClassroom());
                for (String s : selfTeachCourseTimeCO.getClassroom()) {
                    courInfDO.setLocation(s);
                    courseChangeList.add(ObjectUtil.clone(courInfDO));
                }
            }
        }
        List<CourInfDO> difference = getDifference(courseChangeList, courInfoList);
        List<CourInfDO> difference2 = getDifference(courInfoList, courseChangeList);
        if (difference.isEmpty()) {
            for (CourInfDO courInfDO : difference2) {
                courInfMapper.delete(new QueryWrapper<CourInfDO>()
                        .eq("course_id", courInfDO.getCourseId())
                        .eq("week", courInfDO.getWeek()).eq("day", courInfDO.getDay())
                        .eq("start_time", courInfDO.getStartTime()).eq("end_time", courInfDO.getEndTime())
                        .eq("location", courInfDO.getLocation()));
                evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courInfDO.getId()))
                        .forEach(evaTaskDO -> taskMap.put(evaTaskDO.getId(), evaTaskDO.getTeacherId()));
               /* EvaTaskDO evaTaskDO=new EvaTaskDO();
                evaTaskDO.setStatus(2);*/
                evaTaskMapper.delete(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courInfDO.getId()));
            }
            localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(courseDO.getSemesterId()));
            if (taskMap.isEmpty()) {
                return "";
            } else {
                return msg + userDO.getName() + "老师的" + selfTeachCourseCO.getName() + "课程的上课时间（教室）被修改了," + "因而取消您对该课程的评教任务";
            }
        } else {
            for (CourInfDO courInfDO : difference2) {
                courInfMapper.delete(new QueryWrapper<CourInfDO>()
                        .eq("course_id", courInfDO.getCourseId())
                        .eq("week", courInfDO.getWeek()).eq("day", courInfDO.getDay())
                        .eq("start_time", courInfDO.getStartTime()).eq("end_time", courInfDO.getEndTime())
                        .eq("location", courInfDO.getLocation()));
                evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courInfDO.getId()))
                        .forEach(evaTaskDO -> taskMap.put(evaTaskDO.getId(), evaTaskDO.getTeacherId()));
           /*     EvaTaskDO evaTaskDO=new EvaTaskDO();
                evaTaskDO.setStatus(2);*/
                evaTaskMapper.delete(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courInfDO.getId()));
            }
            localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(courseDO.getSemesterId()));
            for (CourInfDO courInfDO : difference) {
                for (CourseDO course : courseDOList) {
                    QueryWrapper<CourInfDO> wrapper = new QueryWrapper<CourInfDO>()
                            .eq("week", courInfDO.getWeek())
                            .eq("day", courInfDO.getDay())
                            .le("start_time", courInfDO.getEndTime())
                            .ge("end_time", courInfDO.getStartTime())
                            .eq("course_id", course.getId());
                    // 判断对应时间段是否已经有课了
                    if (!courInfMapper.selectList(wrapper).isEmpty()) {
                        throw new UpdateException("该时间段你已经有课了");
                    }
                }

                // 评教
                if (!courInfoIds.isEmpty()) {
                    QueryWrapper<CourInfDO> wrapper = new QueryWrapper<CourInfDO>()
                            .eq("week", courInfDO.getWeek())
                            .eq("day", courInfDO.getDay())
                            .le("start_time", courInfDO.getEndTime())
                            .ge("end_time", courInfDO.getStartTime())
                            .in(true, "course_id", courInfoIds);
                    if (!courInfMapper.selectList(wrapper).isEmpty()) {
                        throw new UpdateException("该时间段你有要去评教的课程");
                    }
                }

                // 判断对应时间段的教室是否被占用
                QueryWrapper<CourInfDO> wrapper = new QueryWrapper<CourInfDO>()
                        .eq("week", courInfDO.getWeek())
                        .eq("day", courInfDO.getDay())
                        .le("start_time", courInfDO.getEndTime())
                        .ge("end_time", courInfDO.getStartTime())
                        .and(courseWrapper -> courseWrapper.ne("course_id", courseDO.getId()));
                wrapper.eq("location", courInfDO.getLocation());
                if (courInfMapper.selectOne(wrapper) != null) {
                    // 被占用了，抛出异常
                    throw new UpdateException("该时间段教室已占用");
                }
                courInfMapper.insert(courInfDO);
            }
            return msg + userDO.getName() + "老师的" + selfTeachCourseCO.getName() + "课程的上课时间（教室）被修改了," + "因而取消您对该课程的评教任务";
        }
    }

    public List<CourInfDO> getDifference(List<CourInfDO> courseChangeList, List<CourInfDO> courInfoList) {
        return courseChangeList.stream()
                .filter(courseChange -> courInfoList.stream()
                        .noneMatch(courInfo -> Objects.equals(courseChange.getWeek(), courInfo.getWeek())
                                && Objects.equals(courseChange.getDay(), courInfo.getDay())
                                && Objects.equals(courseChange.getStartTime(), courInfo.getStartTime())
                                && Objects.equals(courseChange.getEndTime(), courInfo.getEndTime())
                                && Objects.equals(courseChange.getLocation(), courInfo.getLocation())))
                .collect(Collectors.toList());
    }

    private String JudgeCourseType(String info, CourseDO courseDO, SelfTeachCourseCO selfTeachCourseCO) {
        String msg = "";
        // 获取课程类型ID
        List<Integer> typeIdDo = courseTypeCourseMapper.selectList(new QueryWrapper<CourseTypeCourseDO>().eq("course_id", courseDO.getId()))
                .stream()
                .map(CourseTypeCourseDO::getTypeId)
                .sorted()
                .collect(Collectors.toList());

        // 获取自定义课程类型的名称
        List<String> typeName = selfTeachCourseCO.getTypeList().stream()
                .map(CourseType::getName)
                .collect(Collectors.toList());

        // 获取数据库中的类型ID
        List<Integer> typeIdIn = courseTypeMapper.selectList(new QueryWrapper<CourseTypeDO>().in(!typeName.isEmpty(), "name", typeName))
                .stream()
                .map(CourseTypeDO::getId)
                .sorted()
                .toList();

        // 比较两个集合
        if (typeIdIn.equals(typeIdDo)) {
            return msg;
        } else {
            courseTypeCourseMapper.delete(new QueryWrapper<CourseTypeCourseDO>().eq("course_id", courseDO.getId()).in(!typeIdDo.isEmpty(), "type_id", typeIdDo));

            typeIdIn.forEach(typeId -> {
                CourseTypeCourseDO courseTypeCourseDO = new CourseTypeCourseDO();
                courseTypeCourseDO.setCourseId(courseDO.getId());
                courseTypeCourseDO.setTypeId(typeId);
                courseTypeCourseDO.setCreateTime(LocalDateTime.now());
                courseTypeCourseDO.setUpdateTime(LocalDateTime.now());
                courseTypeCourseMapper.insert(courseTypeCourseDO);
            });

            return msg + info + "课程类型被修改为:" + String.join(",", typeName) + "。";
        }

    }

    private String toJudge(CourseDO courseDO, SubjectDO subjectDO, SelfTeachCourseCO selfTeachCourseCO) {
        String msg = "";
        String name = subjectDO.getName();
        //0: 理论课相关默认；1: 实验课相关默认；
        String natureExp = subjectDO.getNature().equals(0) ? "理论课" : "实践课";
        if (!subjectDO.getName().equals(selfTeachCourseCO.getName()) || !subjectDO.getNature().equals(selfTeachCourseCO.getNature())) {
            SubjectDO subject = new SubjectDO();
            subject.setNature(selfTeachCourseCO.getNature());
            subject.setName(selfTeachCourseCO.getName());
            subject.setCreateTime(LocalDateTime.now());
            subject.setUpdateTime(LocalDateTime.now());
            subjectMapper.insert(subject);
            //顺便将课程的subjectId更新
            CourseDO course = new CourseDO();

            course.setSubjectId(subject.getId());
            courseMapper.update(course, new QueryWrapper<CourseDO>().eq("id", selfTeachCourseCO.getId()));
            SysUserDO userDO = userMapper.selectById(courseDO.getTeacherId());
            localCacheManager.invalidateCache(courseCacheConstants.SUBJECT_LIST, courseCacheConstants.COURSE_LIST_BY_SEM + courseDO.getSemesterId());
            return msg + userDO.getName() + "老师的" + name + "课程的名称被改成了" + subjectDO.getName() + "，类型是" + natureExp + "。";
        }
        return "";
    }
}
