package edu.cuit.infra.bciam.adapter;

import edu.cuit.bc.iam.application.port.UserNameDirectQueryPort;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * bc-iam：用户姓名直查端口适配器（保持历史行为不变）。
 *
 * <p>约束：用于替代其它 BC 的 {@code sysUserMapper.selectById(...).getName()} 跨 BC 直连写法；
 * 不引入新的缓存/切面副作用（不委托带缓存注解的旧 gateway）。</p>
 */
@Component
@RequiredArgsConstructor
public class UserNameDirectQueryPortImpl implements UserNameDirectQueryPort {

    private final SysUserMapper userMapper;

    @Override
    public String findNameById(Integer id) {
        SysUserDO sysUserDO = userMapper.selectById(id);
        return sysUserDO.getName();
    }
}

