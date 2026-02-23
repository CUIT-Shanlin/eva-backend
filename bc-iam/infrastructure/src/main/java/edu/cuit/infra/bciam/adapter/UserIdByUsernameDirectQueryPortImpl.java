package edu.cuit.infra.bciam.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.bc.iam.application.port.UserIdByUsernameDirectQueryPort;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * bc-iam：用户ID直查端口适配器（按用户名，保持历史行为不变）。
 *
 * <p>约束：用于替代其它 BC 的 {@code sysUserMapper.selectOne(eq username)} 跨 BC 直连写法；
 * 不引入新的缓存/切面副作用（不委托带缓存注解的旧 gateway）。</p>
 */
@Component
@RequiredArgsConstructor
public class UserIdByUsernameDirectQueryPortImpl implements UserIdByUsernameDirectQueryPort {

    private final SysUserMapper userMapper;

    @Override
    public Integer findIdByUsername(String username) {
        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getId).eq(SysUserDO::getUsername, username);
        SysUserDO user = userMapper.selectOne(userQuery);
        return user == null ? null : user.getId();
    }
}

