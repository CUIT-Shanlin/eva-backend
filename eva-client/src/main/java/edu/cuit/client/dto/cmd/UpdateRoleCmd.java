package edu.cuit.client.dto.cmd;

import edu.cuit.client.dto.clientobject.user.RoleDetailCO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 *
 * 新建/修改用户模型(新建/修改用户模型)
 */
@Data
@Accessors(chain = true)
public class UpdateRoleCmd  {
    /**
     * 角色id
     */
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
    private Long status;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
