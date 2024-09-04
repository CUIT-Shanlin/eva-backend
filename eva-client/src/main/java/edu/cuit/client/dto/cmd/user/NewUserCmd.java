package edu.cuit.client.dto.cmd.user;

import com.alibaba.cola.dto.Command;
import edu.cuit.common.validator.status.ValidStatus;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "用户名不能为空")
    private String username;

    /**
     * 昵称
     */
    @NotNull(message = "昵称不能为空")
    private String name;

    /**
     * 密码，明文密码
     */
    @NotNull(message = "密码不能为空")
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
    @NotNull(message = "状态不能为空")
    @ValidStatus(message = "状态值只能为0或1")
    private Integer status;


}
