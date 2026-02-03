package edu.cuit.app.service.impl.user;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.cola.exception.BizException;
import edu.cuit.bc.iam.application.usecase.ValidateUserLoginUseCase;
import edu.cuit.bc.iam.application.contract.api.user.IUserAuthService;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.UserLoginCmd;
import edu.cuit.bc.iam.application.port.UserEntityByUsernameQueryPort;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements IUserAuthService {

    private final LdapPersonGateway ldapPersonGateway;
    private final UserEntityByUsernameQueryPort userEntityByUsernameQueryPort;

    @Override
    public Pair<String, String> login(UserLoginCmd loginCmd) {
        if (StpUtil.isLogin()) {
            throw new BizException("您已经登录过了");
        }
        new ValidateUserLoginUseCase(ldapPersonGateway, userEntityByUsernameQueryPort).execute(loginCmd);
        StpUtil.login(loginCmd.getUsername(), loginCmd.getRememberMe());
        return Pair.of("token", StpUtil.getTokenValue());
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }
}
