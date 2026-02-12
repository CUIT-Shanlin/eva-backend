package edu.cuit.infra.bccourse.adapter;

import edu.cuit.bc.course.application.port.CourseIdByCourInfIdQueryPort;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * bc-course：通过课程详情ID（cour_inf.id）查询课程ID的端口适配器。
 * <p>
 * 约束：保持行为不变；仅提供最小查询能力，用于后续“跨 BC 直连清零”。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class CourseIdByCourInfIdQueryPortImpl implements CourseIdByCourInfIdQueryPort {
    private final CourInfMapper courInfMapper;

    @Override
    public Optional<Integer> findCourseIdByCourInfId(Integer courInfId) {
        if (courInfId == null) {
            return Optional.empty();
        }
        CourInfDO courInf = courInfMapper.selectById(courInfId);
        if (courInf == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(courInf.getCourseId());
    }
}
