package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.CourseIdsBySemesterIdQueryPort;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * bc-course：按 semesterId 查询课程ID集合的查询端口适配器。
 *
 * <p>约束：保持行为不变；内部仅委托 {@link CourseMapper#selectList}。</p>
 */
@Component
@RequiredArgsConstructor
public class CourseIdsBySemesterIdQueryPortImpl implements CourseIdsBySemesterIdQueryPort {
    private final CourseMapper courseMapper;

    @Override
    public List<Integer> findCourseIdsBySemesterId(Integer semesterId) {
        List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semesterId));
        return courseDOS.stream().map(CourseDO::getId).toList();
    }
}
