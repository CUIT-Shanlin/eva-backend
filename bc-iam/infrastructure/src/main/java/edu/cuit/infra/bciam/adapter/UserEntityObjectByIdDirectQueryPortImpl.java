package edu.cuit.infra.bciam.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.iam.application.port.UserEntityObjectByIdDirectQueryPort;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.infra.convertor.user.RoleConverter;
import edu.cuit.infra.convertor.user.UserConverter;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserRoleDO;
import edu.cuit.infra.dal.database.mapper.user.SysRoleMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * bc-iam：用户实体对象（含角色信息）按 ID 查询端口适配器（保持历史行为不变）。
 *
 * <p>约束（保持行为不变）：该适配器用于承接历史“跨 BC 直连 IAM 表”的查询逻辑，保持 SQL 与空值/异常语义一致；
 * 不引入新的缓存/切面副作用（不委托带缓存注解的旧 gateway）。</p>
 */
@Component
@RequiredArgsConstructor
public class UserEntityObjectByIdDirectQueryPortImpl implements UserEntityObjectByIdDirectQueryPort {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final RoleConverter roleConverter;
    private final UserConverter userConverter;

    @Override
    public Object findById(Integer id) {
        SysUserDO sysUserDO = userMapper.selectById(id);
        List<Integer> roleIds = userRoleMapper.selectList(new QueryWrapper<SysUserRoleDO>().eq("user_id", id))
                .stream().map(SysUserRoleDO::getRoleId).toList();
        List<SysRoleDO> roleList = roleIds.isEmpty() ? List.of() : roleMapper.selectList(new QueryWrapper<SysRoleDO>().in("id", roleIds));
        List<RoleEntity> roleEntities = roleList.stream().map(roleConverter::toRoleEntity).toList();
        return userConverter.toUserEntityObject(sysUserDO, () -> roleEntities);
    }
}

