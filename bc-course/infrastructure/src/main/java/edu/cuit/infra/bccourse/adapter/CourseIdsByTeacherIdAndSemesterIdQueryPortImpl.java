package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.CourseIdsByTeacherIdAndSemesterIdQueryPort;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link CourseIdsByTeacherIdAndSemesterIdQueryPort} 的端口适配器实现（保持行为不变）。
 *
 * <p>说明：该适配器仅做“原样委托 + 映射返回”，不引入新的缓存/日志副作用。</p>
 */
@Component
@RequiredArgsConstructor
public class CourseIdsByTeacherIdAndSemesterIdQueryPortImpl implements CourseIdsByTeacherIdAndSemesterIdQueryPort {

    private final CourseMapper courseMapper;

    @Override
    public List<Integer> findCourseIdsByTeacherIdAndSemesterId(Integer teacherId, Integer semesterId) {
        return courseMapper.selectList(new QueryWrapper<CourseDO>()
                        .eq("teacher_id", teacherId)
                        .eq("semester_id", semesterId))
                .stream()
                .map(CourseDO::getId)
                .toList();
    }
}

