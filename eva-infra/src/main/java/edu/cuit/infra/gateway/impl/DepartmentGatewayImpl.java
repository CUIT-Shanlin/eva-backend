package edu.cuit.infra.gateway.impl;

import edu.cuit.domain.gateway.DepartmentGateway;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import lombok.RequiredArgsConstructor;

import java.util.List;
@RequiredArgsConstructor
public class DepartmentGatewayImpl implements DepartmentGateway {
    private final SysUserMapper userMapper;
    @Override
    public List<String> getAll() {
        List<SysUserDO> sysUserDOS = userMapper.selectList(null);
        //根据角色的院系进行分类(去重)
        List<String> list = sysUserDOS.stream().map(SysUserDO::getDepartment).distinct().toList();
        return list;
    }
}
