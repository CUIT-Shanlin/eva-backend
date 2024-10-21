package edu.cuit.infra.gateway.impl;

import edu.cuit.domain.gateway.DepartmentGateway;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DepartmentGatewayImpl implements DepartmentGateway {
    private final SysUserMapper userMapper;
    @Override
    public List<String> getAll() {
        List<SysUserDO> sysUserDOS = userMapper.selectList(null);
        if(sysUserDOS.isEmpty())throw new QueryException("暂时还没有用院系信息");
        //根据角色的院系进行分类(去重)
        return sysUserDOS.stream().map(SysUserDO::getDepartment).distinct().toList();
    }
}
