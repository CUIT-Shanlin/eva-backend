package edu.cuit.client.dto.cmd.user;

import com.alibaba.cola.dto.Command;
import edu.cuit.common.enums.GenericPattern;
import edu.cuit.client.validator.status.ValidStatus;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

/**
 * 修改用户模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class UpdateUserCmd extends Command {

    /**
     * 用户id
     */
    @NotNull(message = "用户ID不能为空")
    private Long id;

    /**
     * 用户名(需更新ldap)
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 系(需更新ldap)
     */
    private String department;

    /**
     * 邮箱(需更新ldap)
     */
    @Email(message = "邮箱不符合格式")
    private String email;

    /**
     * 昵称(需更新ldap)
     */
    @NotBlank(message = "名称不能为空")
    private String name;

    /**
     * 密码，明文密码(需更新ldap)
     */
    @NotBlank(message = "密码不能为空")
    @Length(min = 4,message = "密码长度不能小于4")
    private String password;

    /**
     * 手机号(需更新ldap)
     */
    private String phone;

    /**
     * 职称(需更新ldap)
     */
    private String profTitle;

    /**
     * 状态，1为禁止，0为正常
     */
    @ValidStatus(message = "用户状态只能是0或1")
    private Integer status;

}
