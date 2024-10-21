package edu.cuit.client.api.user;

import edu.cuit.client.dto.cmd.user.UserLoginCmd;
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
