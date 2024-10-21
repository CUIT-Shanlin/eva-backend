package edu.cuit.client.dto.cmd.user;

import com.alibaba.cola.dto.Command;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 登录模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserLoginCmd extends Command {

    /**
     * 用户名或手机号
     */
    @NotNull(message = "用户名不能为空")
    private String username;

    /**
     * 登录密码（未加密）
     */
    @NotNull(message = "密码不能为空")
    private String password;

    /**
     * 是否自动登录
     */
    @NotNull(message = "是否记住我选项不能为空")
    private Boolean rememberMe;
}
