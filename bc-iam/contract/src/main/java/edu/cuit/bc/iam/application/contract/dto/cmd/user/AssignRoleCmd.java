package edu.cuit.bc.iam.application.contract.dto.cmd.user;

import com.alibaba.cola.dto.Command;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 角色分配模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class AssignRoleCmd extends Command {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    /**
     * 角色ID列表
     */
    private List<Integer> roleIdList;
}
