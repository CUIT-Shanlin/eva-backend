package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.CourseIdByTemplateIdQueryPort;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * bc-course：按评教模板ID查询“任一课程ID”的查询端口适配器（用于判断模板是否已被课程分配）。
 *
 * <p>保持行为不变（重要）：沿用调用方旧实现语义（QueryWrapper 条件与空值表现不变），不引入新的缓存/日志副作用。</p>
 */
@Component
@RequiredArgsConstructor
public class CourseIdByTemplateIdQueryPortImpl implements CourseIdByTemplateIdQueryPort {
    private final CourseMapper courseMapper;

    @Override
    public Optional<Integer> findCourseIdByTemplateId(Integer templateId) {
        QueryWrapper<CourseDO> qw = new QueryWrapper<CourseDO>().eq("templateId", templateId);
        CourseDO course = courseMapper.selectOne(qw);
        if (course == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(course.getId());
    }
}
