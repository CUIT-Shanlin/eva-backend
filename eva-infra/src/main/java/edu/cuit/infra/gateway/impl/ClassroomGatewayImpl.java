package edu.cuit.infra.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.domain.gateway.ClassroomGateway;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.zhuyimeng.framework.cache.aspect.annotation.local.LocalCached;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ClassroomGatewayImpl implements ClassroomGateway {

    private final CourInfMapper courInfMapper;

    @Override
    @LocalCached(key = "#{@classroomCacheConstants.ALL_CLASSROOM}")
    public List<String> getAll() {
        LambdaQueryWrapper<CourInfDO> courInfQuery = Wrappers.lambdaQuery();
        courInfQuery.select(CourInfDO::getLocation);
        return courInfMapper.selectList(courInfQuery).stream()
                .map(CourInfDO::getLocation)
                .distinct()
                .sorted()
                .toList();
    }
}
