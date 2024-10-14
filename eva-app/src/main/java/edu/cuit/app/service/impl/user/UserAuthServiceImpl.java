package edu.cuit.app.service.impl.user;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.cola.exception.BizException;
import edu.cuit.client.api.user.IUserAuthService;
import edu.cuit.client.dto.cmd.user.UserLoginCmd;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements IUserAuthService {

    private final LdapPersonGateway ldapPersonGateway;

    @Override
    public Pair<String, String> login(UserLoginCmd loginCmd) {
        if (ldapPersonGateway.authenticate(loginCmd.getUsername(),loginCmd.getPassword())) {
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
