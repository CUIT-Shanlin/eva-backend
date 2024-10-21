package edu.cuit.client.dto.cmd.user;

import com.alibaba.cola.dto.Command;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 权限分配模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class AssignPermCmd extends Command {

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    private Integer roleId;

    /**
     * 菜单ID列表
     */
    private List<Integer> menuIdList;
}
