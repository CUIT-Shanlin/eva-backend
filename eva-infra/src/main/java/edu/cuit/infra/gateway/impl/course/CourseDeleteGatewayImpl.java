package edu.cuit.infra.gateway.impl.course;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import edu.cuit.client.dto.data.course.CoursePeriod;
import edu.cuit.domain.gateway.course.CourseDeleteGateway;
import edu.cuit.infra.dal.database.dataobject.course.*;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.*;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.enums.cache.ClassroomCacheConstants;
import edu.cuit.infra.enums.cache.CourseCacheConstants;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.infra.gateway.impl.course.operate.CourseFormat;
import edu.cuit.bc.course.application.model.DeleteSelfCourseCommand;
import edu.cuit.bc.course.application.usecase.DeleteSelfCourseUseCase;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CourseDeleteGatewayImpl implements CourseDeleteGateway {
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final CourseTypeCourseMapper courseTypeCourseMapper;
    private final CourseTypeMapper courseTypeMapper;
    private final SubjectMapper subjectMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final SysUserMapper userMapper;
    private final LocalCacheManager localCacheManager;
    private final CourseCacheConstants courseCacheConstants;
    private final FormRecordMapper formRecordMapper;
    private final EvaCacheConstants evaCacheConstants;
    private final ClassroomCacheConstants classroomCacheConstants;
    private final DeleteSelfCourseUseCase deleteSelfCourseUseCase;


    /**
     * 批量删除某节课
     *
     * @param semId        学期id
     * @param id           对应课程编号
     * @param coursePeriod 课程的一段时间模型
     */
    @Override
    @Transactional
    public Map<String, Map<Integer, Integer>> deleteCourses(Integer semId, Integer id, CoursePeriod coursePeriod) {
        CourInfDO courInfDO = courInfMapper.selectById(id);
        if (courInfDO == null) throw new UpdateException("该节课不存在");
        id = courInfDO.getCourseId();
        CourseDO courseDO = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", id).eq("semester_id", semId));
        if (courseDO == null) {
            throw new QueryException("课程不存在");
        }
        SysUserDO userDO = userMapper.selectById(courseDO.getTeacherId());
        if (userDO == null) throw new QueryException("对应老师不存在");
        SubjectDO subjectDO = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", courseDO.getSubjectId()));
        String natureName = CourseFormat.getNatureName(subjectDO.getNature());
        String name = subjectDO.getName();
        //id来找出课程数据
        QueryWrapper<CourInfDO> courseWrapper = new QueryWrapper<>();
        courseWrapper.eq("course_id", id);
        isEmptiy(courseWrapper, coursePeriod);
        List<Integer> list = courInfMapper.selectList(courseWrapper).stream().map(CourInfDO::getId).toList();
        courInfMapper.delete(courseWrapper);
        //找出所有要评教这节课的老师
        List<EvaTaskDO> tasks;
        if (!list.isEmpty()) {
            tasks = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id", list));
            evaTaskMapper.delete(new QueryWrapper<EvaTaskDO>().in("cour_inf_id", list));
        } else {
            tasks = new ArrayList<>();
        }
        //删除评教记录
        if (!tasks.isEmpty()) {
            formRecordMapper.delete(new QueryWrapper<FormRecordDO>().in("task_id", tasks.stream().map(EvaTaskDO::getId).toList()));
        }
        Map<Integer, Integer> mapEva = new HashMap<>();
        for (EvaTaskDO task : tasks) {
            mapEva.put(task.getId(), task.getTeacherId());
        }
        Map<String, Map<Integer, Integer>> map = new HashMap<>();
        map.put(userDO.getName() + "老师的" + name + "课程("+natureName+")的一些课程已被删除", null);
        map.put("你所评教的" + userDO.getName() + "老师的" + "上课时间在第" + coursePeriod.getStartWeek() + "周，星期" + coursePeriod.getDay()
                + "，第" + coursePeriod.getStartTime() + "-" + coursePeriod.getEndTime() + "节，" + name + "课程已经被删除，故已取消您对该课程的评教任务", mapEva);
        LogUtils.logContent(userDO.getName() + "老师-" + name + "(课程ID:" + id + ")的一些课");
        localCacheManager.invalidateCache(null, evaCacheConstants.LOG_LIST);
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId));
        localCacheManager.invalidateCache(null, classroomCacheConstants.ALL_CLASSROOM);
        return map;
    }


    /**
     * 连带删除一门课程
     *
     * @param semId 学期id
     * @param id    对应课程编号
     */
    @Override
    @Transactional
    public Map<String, Map<Integer, Integer>> deleteCourse(Integer semId, Integer id) {
        //删除课程表
        QueryWrapper<CourseDO> courseWrapper = new QueryWrapper<>();
        courseWrapper.eq("id", id);
        if (semId != null) {
            courseWrapper.eq("semester_id", semId);
        }
        CourseDO courseDO = courseMapper.selectOne(courseWrapper);
        if (courseDO == null) throw new QueryException("课程已经被删除，或者不存在");
        SysUserDO userDO = userMapper.selectById(courseDO.getTeacherId());
        SubjectDO subjectDO = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", courseDO.getSubjectId()));
        String natureName =CourseFormat.getNatureName(subjectDO.getNature());
        String name = subjectDO.getName();
        int delete = courseMapper.delete(courseWrapper);
        if (delete == 0) {
            throw new UpdateException("该课程不存在");
        }
        if (!courseMapper.exists(new QueryWrapper<CourseDO>().eq("subject_id", courseDO.getSubjectId()))) {
            subjectMapper.delete(new QueryWrapper<SubjectDO>().eq("id", courseDO.getSubjectId()));
            localCacheManager.invalidateCache(null, courseCacheConstants.SUBJECT_LIST);
        }

        localCacheManager.invalidateCache(courseCacheConstants.COURSE_LIST_BY_SEM, String.valueOf(semId));
        //删除课程详情表
        UpdateWrapper<CourInfDO> courInfoWrapper = new UpdateWrapper<>();
        courInfoWrapper.eq("course_id", id);
        List<Integer> list = courInfMapper.selectList(courInfoWrapper).stream().map(CourInfDO::getId).toList();
        courInfMapper.delete(courInfoWrapper);
        //删除评教任务数据
        List<EvaTaskDO> taskDOList;
        if (!list.isEmpty()) {
            QueryWrapper<EvaTaskDO> evaTaskWrapper = new QueryWrapper<>();
            evaTaskWrapper.in("cour_inf_id", list);
            taskDOList = evaTaskMapper.selectList(evaTaskWrapper);
        } else {
            taskDOList = new ArrayList<>();
        }

        List<Integer> taskIds = taskDOList.stream().map(EvaTaskDO::getId).toList();
        List<EvaTaskDO> list1 = taskDOList.stream().filter(taskDO -> taskDO.getStatus() == 0).toList();
        if (!taskIds.isEmpty()) {
            evaTaskMapper.delete(new QueryWrapper<EvaTaskDO>().in("id", taskIds));
            formRecordMapper.delete(new QueryWrapper<FormRecordDO>().in("task_id", taskIds));
        }

        Map<String, Map<Integer, Integer>> map = new HashMap<>();
        Map<Integer, Integer> evaTaskMap = new HashMap<>();
        for (EvaTaskDO taskDO : list1) {
            evaTaskMap.put(taskDO.getId(), taskDO.getTeacherId());
        }

        map.put(userDO.getName()+"老师的"+name + "课程（"+natureName+"）被删除", null);
        map.put("因为" + userDO.getName()+"老师的"+name + "课程已被删除，" + "故已取消您对该课程的评教任务,和评教记录", evaTaskMap);
        LogUtils.logContent(name + "(课程ID:" + id + ")这门课");
        localCacheManager.invalidateCache(null, evaCacheConstants.LOG_LIST);
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId));
        localCacheManager.invalidateCache(null, classroomCacheConstants.ALL_CLASSROOM);
        return map;
    }


    /**
     * 删除一个课程类型/批量删除课程类型
     *
     * @param ids 课程类型数组
     */
    @Override
    @Transactional
    public Void deleteCourseType(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new UpdateException("请选择要删除的课程类型");
        }
        courseTypeMapper.selectList(new QueryWrapper<CourseTypeDO>().in(true, "id", ids)).forEach(courseTypeDO ->
        {
            if (courseTypeDO.getIsDefault() != -1)
                throw new UpdateException("默认课程类型不能删除");
        });
        QueryWrapper<CourseTypeCourseDO> wrapper = new QueryWrapper<>();
        wrapper.in(!ids.isEmpty(), "type_id", ids);
        //将courseTypeCourse逻辑删除
        courseTypeCourseMapper.delete(wrapper);
        // 将对应课程类型的逻辑删除
        UpdateWrapper<CourseTypeDO> courseTypeWrapper = new UpdateWrapper<>();
        courseTypeWrapper.in(!ids.isEmpty(), "id", ids);
        List<CourseTypeDO> courseTypeDOS = courseTypeMapper.selectList(courseTypeWrapper);
        courseTypeMapper.delete(courseTypeWrapper);
        courseTypeDOS.forEach(courseType -> LogUtils.logContent(courseType.getName() + "课程类型"));
        localCacheManager.invalidateCache(null, courseCacheConstants.COURSE_TYPE_LIST);
        return null;
    }

    @Override
    @Transactional
    public Map<String, Map<Integer, Integer>> deleteSelfCourse(String userName, Integer courseId) {
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“自助删课”业务流程（行为不变）
        return deleteSelfCourseUseCase.execute(new DeleteSelfCourseCommand(userName, courseId));
    }

    private void isEmptiy(QueryWrapper wrapper, CoursePeriod coursePeriod) {
        if (coursePeriod.getStartWeek() != null) {
            wrapper.ge("week", coursePeriod.getStartWeek());
        }
        if (coursePeriod.getEndWeek() != null) {
            wrapper.le("week", coursePeriod.getEndWeek());
        }
        if (coursePeriod.getDay() != null) {
            wrapper.eq("day", coursePeriod.getDay());
        }
        if (coursePeriod.getStartTime() != null) {
            wrapper.eq("start_time", coursePeriod.getStartTime());
        }
        if (coursePeriod.getEndTime() != null) {
            wrapper.eq("end_time", coursePeriod.getEndTime());
        }
    }

}
