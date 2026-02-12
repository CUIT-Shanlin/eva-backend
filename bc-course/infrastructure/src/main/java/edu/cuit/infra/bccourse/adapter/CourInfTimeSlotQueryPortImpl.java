package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.CourInfTimeSlotQueryPort;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * bc-course：课程详情（cour_inf）时间片查询端口适配器。
 * <p>
 * 约束：保持行为不变；内部仅委托 {@link CourInfMapper#selectById}/{@link CourInfMapper#selectList}。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class CourInfTimeSlotQueryPortImpl implements CourInfTimeSlotQueryPort {
    private final CourInfMapper courInfMapper;

    @Override
    public Optional<CourInfTimeSlot> findByCourInfId(Integer courInfId) {
        if (courInfId == null) {
            return Optional.empty();
        }
        CourInfDO courInf = courInfMapper.selectById(courInfId);
        if (courInf == null) {
            return Optional.empty();
        }
        return Optional.of(toTimeSlot(courInf));
    }

    @Override
    public List<CourInfTimeSlot> findByCourseIds(List<Integer> courseIds) {
        List<CourInfDO> courInfDOS = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id", courseIds));
        return courInfDOS.stream().map(this::toTimeSlot).toList();
    }

    @Override
    public List<CourInfTimeSlot> findByCourInfIds(List<Integer> courInfIds) {
        List<CourInfDO> courInfDOS = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("id", courInfIds));
        return courInfDOS.stream().map(this::toTimeSlot).toList();
    }

    private CourInfTimeSlot toTimeSlot(CourInfDO courInf) {
        return new CourInfTimeSlot(
                courInf.getId(),
                courInf.getCourseId(),
                courInf.getWeek(),
                courInf.getDay(),
                courInf.getStartTime(),
                courInf.getEndTime()
        );
    }
}

