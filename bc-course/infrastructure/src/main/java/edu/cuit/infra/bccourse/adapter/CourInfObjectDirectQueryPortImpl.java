package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.CourInfObjectDirectQueryPort;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * bc-course：课程详情对象（cour_inf）直查端口适配器。
 * <p>
 * 约束：保持行为不变；内部仅委托 {@link CourInfMapper#selectById}/{@link CourInfMapper#selectList}。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class CourInfObjectDirectQueryPortImpl implements CourInfObjectDirectQueryPort {
    private final CourInfMapper courInfMapper;

    @Override
    public Object findById(Integer courInfId) {
        return courInfMapper.selectById(courInfId);
    }

    @Override
    public List<Object> findByCourseIds(List<Integer> courseIds) {
        List<CourInfDO> courInfDOS = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id", courseIds));
        return courInfDOS.stream().map(it -> (Object) it).toList();
    }

    @Override
    public List<Object> findByIds(List<Integer> courInfIds) {
        List<CourInfDO> courInfDOS = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("id", courInfIds));
        return courInfDOS.stream().map(it -> (Object) it).toList();
    }
}

