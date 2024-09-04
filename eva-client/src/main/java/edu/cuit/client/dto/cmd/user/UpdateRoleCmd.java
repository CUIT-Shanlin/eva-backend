package edu.cuit.client.dto.cmd.user;

import com.alibaba.cola.dto.Command;
import edu.cuit.common.validator.status.ValidStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 创建/修改角色模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class UpdateRoleCmd extends Command {

    /**
     * 角色id
     */
    @NotNull(message = "角色ID不能为空")
    private Long id;

    /**
     * 描述
     */
    private String description;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 状态(0:禁止,1:正常)
     */
    @ValidStatus(message = "角色状态只能是0或1")
    private Integer status;

}
