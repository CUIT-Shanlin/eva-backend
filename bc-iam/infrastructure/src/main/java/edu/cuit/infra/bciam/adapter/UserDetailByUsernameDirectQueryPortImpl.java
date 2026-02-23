package edu.cuit.infra.bciam.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.bc.iam.application.contract.dto.clientobject.user.UserDetailCO;
import edu.cuit.bc.iam.application.port.UserDetailByUsernameDirectQueryPort;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * bc-iam：用户详情直查端口适配器（按用户名，保持历史行为不变）。
 *
 * <p>约束：用于替代其它 BC 的 {@code sysUserMapper.selectOne(eq username)} 跨 BC 直连写法；
 * 不引入新的缓存/切面副作用（不委托带缓存注解的旧 gateway）。</p>
 */
@Component
@RequiredArgsConstructor
public class UserDetailByUsernameDirectQueryPortImpl implements UserDetailByUsernameDirectQueryPort {

    private final SysUserMapper userMapper;

    @Override
    public Optional<UserDetailCO> findByUsername(String username) {
        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getId, SysUserDO::getName, SysUserDO::getUsername).eq(SysUserDO::getUsername, username);
        SysUserDO user = userMapper.selectOne(userQuery);
        if (user == null) {
            return Optional.empty();
        }

        UserDetailCO detail = new UserDetailCO();
        detail.setId(user.getId() == null ? null : user.getId().longValue());
        detail.setName(user.getName());
        detail.setUsername(user.getUsername());
        return Optional.of(detail);
    }
}

