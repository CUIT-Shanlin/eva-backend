package edu.cuit.app.service.impl.user;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.cola.exception.BizException;
import edu.cuit.client.api.user.IUserAuthService;
import edu.cuit.client.dto.cmd.user.UserLoginCmd;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements IUserAuthService {

    private final LdapPersonGateway ldapPersonGateway;
    private final UserQueryGateway userQueryGateway;

    @Override
    public Pair<String, String> login(UserLoginCmd loginCmd) {
        if (ldapPersonGateway.authenticate(loginCmd.getUsername(),loginCmd.getPassword())) {
            UserEntity user = userQueryGateway.findByUsername(loginCmd.getUsername()).orElseThrow(() -> new BizException("用户名未找到"));
            if (user.getStatus() == 0) {
                throw new BizException("该账户已被停用，请联系管理员");
            }
            StpUtil.login(loginCmd.getUsername(),loginCmd.getRememberMe());
            return Pair.of("token",StpUtil.getTokenValue());
        } else {
            throw new BizException("用户名或密码错误");
        }
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }
}
