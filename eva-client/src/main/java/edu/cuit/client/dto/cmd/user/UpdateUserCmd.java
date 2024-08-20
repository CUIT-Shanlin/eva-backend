package edu.cuit.client.dto.cmd.user;

import com.alibaba.cola.dto.Command;
import edu.cuit.client.dto.clientobject.user.RoleDetailCO;
import edu.cuit.client.dto.clientobject.user.UserDetailCO;
import edu.cuit.common.enums.GenericPattern;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

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
    private String name;

    /**
     * 密码，明文密码(需更新ldap)
     */
    private String password;

    /**
     * 手机号(需更新ldap)
     */
    @Pattern(regexp = GenericPattern.CHINA_PHONE,message = "手机号不符合格式")
    private String phone;

    /**
     * 职称(需更新ldap)
     */
    private String profTitle;

    /**
     * 状态，1为禁止，0为正常
     */
    @Min(value = 0,message = "角色状态只能是0或1")
    @Max(value = 1,message = "角色状态只能是0或1")
    private Long status;

}
