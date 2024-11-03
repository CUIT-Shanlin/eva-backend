package edu.cuit.infra.gateway.impl;

import edu.cuit.domain.gateway.DepartmentGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.zhuyimeng.framework.cache.aspect.annotation.local.LocalCached;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class DepartmentGatewayImpl implements DepartmentGateway {

    private final SysUserMapper userMapper;

    @Override
    @LocalCached(key = "#{@userCacheConstants.ALL_DEPARTMENT}")
    public List<String> getAll() {
        List<SysUserDO> sysUserDOS = userMapper.selectList(null);
        //根据角色的院系进行分类(去重)
        return sysUserDOS.stream().map(SysUserDO::getDepartment)
                .filter(Objects::nonNull)
                .distinct().toList();
    }
}
