package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.CourseTemplateIdQueryPort;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * bc-course：课程模板ID查询端口适配器（用于判断 templateId 是否真的发生切换）。
 */
@Component
@RequiredArgsConstructor
public class CourseTemplateIdQueryPortImpl implements CourseTemplateIdQueryPort {
    private final CourseMapper courseMapper;

    @Override
    public Optional<Integer> findTemplateId(Integer semesterId, Integer courseId) {
        if (courseId == null) {
            return Optional.empty();
        }
        QueryWrapper<CourseDO> qw = new QueryWrapper<CourseDO>().eq("id", courseId);
        if (semesterId != null) {
            qw.eq("semester_id", semesterId);
        }
        CourseDO course = courseMapper.selectOne(qw);
        if (course == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(course.getTemplateId());
    }
}

