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
import edu.cuit.bc.course.application.model.DeleteCourseCommand;
import edu.cuit.bc.course.application.model.DeleteCoursesCommand;
import edu.cuit.bc.course.application.usecase.DeleteCourseUseCase;
import edu.cuit.bc.course.application.usecase.DeleteCoursesUseCase;
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
    private final DeleteCoursesUseCase deleteCoursesUseCase;
    private final DeleteCourseUseCase deleteCourseUseCase;


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
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“批量删课”业务流程（行为不变）
        return deleteCoursesUseCase.execute(new DeleteCoursesCommand(semId, id, coursePeriod));
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
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“删课”业务流程（行为不变）
        return deleteCourseUseCase.execute(new DeleteCourseCommand(semId, id));
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
