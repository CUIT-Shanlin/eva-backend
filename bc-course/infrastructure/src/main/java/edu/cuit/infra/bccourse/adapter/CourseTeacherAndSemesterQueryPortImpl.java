package edu.cuit.infra.bccourse.adapter;

import edu.cuit.bc.course.application.port.CourseTeacherAndSemesterQueryPort;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * bc-course：按课程ID查询任课教师与学期信息的查询端口适配器。
 *
 * <p>约束：保持行为不变；内部仅委托 {@link CourseMapper#selectById}。</p>
 */
@Component
@RequiredArgsConstructor
public class CourseTeacherAndSemesterQueryPortImpl implements CourseTeacherAndSemesterQueryPort {
    private final CourseMapper courseMapper;

    @Override
    public Optional<CourseTeacherAndSemester> findByCourseId(Integer courseId) {
        CourseDO courseDO = courseMapper.selectById(courseId);
        if (courseDO == null) {
            return Optional.empty();
        }
        return Optional.of(new CourseTeacherAndSemester(courseDO.getTeacherId(), courseDO.getSemesterId()));
    }
}

