package edu.cuit.client.dto.query.login;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录模型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToLogin {
    /**
     * 用户名或手机号
     */
    private String username;
    //登录密码

    private String password;
    /**
     * 是否为记住我模式
     */
    private Boolean rememberMe;
}
