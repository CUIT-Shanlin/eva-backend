package edu.cuit.client.dto.cmd.user;

import com.alibaba.cola.dto.Command;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 新建用户模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class NewUserCmd extends Command {

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String name;

    /**
     * 密码，明文密码
     */
    private String password;

    /**
     * 系
     */
    private String department;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 职称
     */
    private String profTitle;

    /**
     * 状态，1为禁止，0为正常
     */
    private Integer status;


}
