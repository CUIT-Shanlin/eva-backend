package edu.cuit.bc.iam.application.usecase;

import com.alibaba.cola.exception.BizException;
import edu.cuit.bc.iam.application.port.UserEntityByUsernameQueryPort;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.UserLoginCmd;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;

/**
 * 用户登录校验用例（保持行为不变）。
 *
 * <p>说明：本用例仅用于收敛 {@code eva-app} 的旧入口实现，使其退化为“登录态解析 + 委托用例 + 登录态写入”的壳。
 * 这里严格复刻旧入口的异常类型/异常文案与分支顺序，不做任何业务语义调整。</p>
 */
public class ValidateUserLoginUseCase {

    private final LdapPersonGateway ldapPersonGateway;
    private final UserEntityByUsernameQueryPort userEntityByUsernameQueryPort;

    public ValidateUserLoginUseCase(LdapPersonGateway ldapPersonGateway,
                                    UserEntityByUsernameQueryPort userEntityByUsernameQueryPort) {
        this.ldapPersonGateway = ldapPersonGateway;
        this.userEntityByUsernameQueryPort = userEntityByUsernameQueryPort;
    }

    /**
     * 过渡构造：保持历史调用点不变（仍可传入旧 gateway），用于逐步收敛依赖方对旧 gateway 的编译期依赖。
     */
    @Deprecated
    public ValidateUserLoginUseCase(LdapPersonGateway ldapPersonGateway, UserQueryGateway userQueryGateway) {
        this(ldapPersonGateway, username -> userQueryGateway.findByUsername(username));
    }

    public void execute(UserLoginCmd loginCmd) {
        if (ldapPersonGateway.authenticate(loginCmd.getUsername(), loginCmd.getPassword())) {
            UserEntity user = (UserEntity) userEntityByUsernameQueryPort.findByUsername(loginCmd.getUsername())
                    .orElseThrow(() -> new BizException("用户名未找到"));
            if (user.getStatus() == 0) {
                throw new BizException("该账户已被停用，请联系管理员");
            }
            return;
        }
        throw new BizException("用户名或密码错误");
    }
}
