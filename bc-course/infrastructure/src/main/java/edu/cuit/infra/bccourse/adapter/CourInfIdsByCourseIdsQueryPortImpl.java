package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.CourInfIdsByCourseIdsQueryPort;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * bc-course：通过课程ID集合（course.id）查询课程详情ID集合（cour_inf.id）的端口适配器。
 * <p>
 * 约束：保持行为不变；内部仅委托 {@link CourInfMapper#selectList}，用于后续“跨 BC 直连清零”。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class CourInfIdsByCourseIdsQueryPortImpl implements CourInfIdsByCourseIdsQueryPort {
    private final CourInfMapper courInfMapper;

    @Override
    public List<Integer> findCourInfIdsByCourseIds(List<Integer> courseIds) {
        List<CourInfDO> courInfDOS = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id", courseIds));
        return courInfDOS.stream().map(CourInfDO::getId).toList();
    }
}

