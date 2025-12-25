package edu.cuit.bc.iam.application.contract.api.user;

import edu.cuit.bc.iam.application.contract.dto.cmd.user.UserLoginCmd;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 用户认证相关业务接口
 */
public interface IUserAuthService {

    /**
     * 登录请求
     * @return key: token,value: token
     */
    Pair<String,String> login(UserLoginCmd loginCmd);

    /**
     * 退出登录
     */
    void logout();

}
