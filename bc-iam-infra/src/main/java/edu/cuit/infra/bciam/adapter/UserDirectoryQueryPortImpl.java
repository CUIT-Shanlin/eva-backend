package edu.cuit.infra.bciam.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.toolkit.MPJWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import edu.cuit.bc.iam.application.port.UserDirectoryQueryPort;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.infra.convertor.user.UserConverter;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserRoleDO;
import edu.cuit.infra.dal.database.mapper.user.SysRoleMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * bc-iam：用户目录查询端口适配器（保持历史行为不变：原样搬运旧 gateway 查询逻辑）。
 *
 * <p>当前收敛目标：把 {@code UserQueryGatewayImpl.findAllUserId/findAllUsername/allUser/getUserRoleIds}
 * 从旧 gateway 中抽离，后续让旧 gateway 逐步退化为委托壳；API/缓存 key 与命中语义保持不变。</p>
 */
@Component
@RequiredArgsConstructor
public class UserDirectoryQueryPortImpl implements UserDirectoryQueryPort {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final UserConverter userConverter;

    @Override
    public List<Integer> findAllUserId() {
        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getId);
        return userMapper.selectList(userQuery).stream().map(SysUserDO::getId).toList();
    }

    @Override
    public List<String> findAllUsername() {
        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getUsername);
        return userMapper.selectList(userQuery).stream().map(SysUserDO::getUsername).toList();
    }

    @Override
    public List<SimpleResultCO> allUser() {
        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getId, SysUserDO::getName);
        return userMapper.selectList(userQuery).stream().map(userConverter::toUserSimpleResult).toList();
    }

    @Override
    public List<Integer> getUserRoleIds(Integer userId) {
        MPJLambdaWrapper<SysRoleDO> roleQuery = MPJWrappers.lambdaJoin();
        roleQuery
                .select(SysRoleDO::getId)
                .innerJoin(SysUserRoleDO.class, on -> on.eq(SysUserRoleDO::getUserId, userId))
                .eq(SysRoleDO::getId, SysUserRoleDO::getRoleId);

        return roleMapper.selectList(roleQuery).stream().map(SysRoleDO::getId).toList();
    }
}

