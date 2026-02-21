package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import edu.cuit.bc.course.application.port.CourseIdsByCourseWrapperDirectQueryPort;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link CourseIdsByCourseWrapperDirectQueryPort} 的端口适配器实现（保持行为不变）。
 *
 * <p>说明：该适配器仅做“原样委托 + 返回”，不引入新的缓存/日志副作用。</p>
 */
@Component
@RequiredArgsConstructor
public class CourseIdsByCourseWrapperDirectQueryPortImpl implements CourseIdsByCourseWrapperDirectQueryPort {
    private final CourseMapper courseMapper;

    @Override
    public List<Integer> findCourseIds(Wrapper<CourseDO> queryWrapper) {
        return courseMapper.selectList(queryWrapper).stream().map(CourseDO::getId).toList();
    }
}

