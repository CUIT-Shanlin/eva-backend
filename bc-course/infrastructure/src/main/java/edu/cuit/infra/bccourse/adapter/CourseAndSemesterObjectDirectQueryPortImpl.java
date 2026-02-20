package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import edu.cuit.bc.course.application.port.CourseAndSemesterObjectDirectQueryPort;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SemesterDO;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SemesterMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link CourseAndSemesterObjectDirectQueryPort} 的端口适配器实现（保持行为不变）。
 *
 * <p>说明：该适配器仅做“原样委托 + 返回”，不引入新的缓存/日志副作用。</p>
 */
@Component
@RequiredArgsConstructor
public class CourseAndSemesterObjectDirectQueryPortImpl implements CourseAndSemesterObjectDirectQueryPort {
    private final CourseMapper courseMapper;
    private final SemesterMapper semesterMapper;

    @Override
    public List<CourseDO> findCourseList(Wrapper<CourseDO> queryWrapper) {
        return courseMapper.selectList(queryWrapper);
    }

    @Override
    public CourseDO findCourseById(Integer courseId) {
        return courseMapper.selectById(courseId);
    }

    @Override
    public CourseDO findOneCourse(Wrapper<CourseDO> queryWrapper) {
        return courseMapper.selectOne(queryWrapper);
    }

    @Override
    public SemesterDO findSemesterById(Integer semesterId) {
        return semesterMapper.selectById(semesterId);
    }
}

