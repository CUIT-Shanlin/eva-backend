package edu.cuit.bc.iam.application.contract.dto.cmd.user;

import com.alibaba.cola.dto.Command;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 新建角色模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class NewRoleCmd extends Command {

    /**
     * 角色名
     */
    @NotBlank(message = "角色名不能为空")
    private String roleName;

    /**
     * 角色描述
     */
    private String description;

}
