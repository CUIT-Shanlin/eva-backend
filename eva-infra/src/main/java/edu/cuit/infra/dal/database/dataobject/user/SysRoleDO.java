package edu.cuit.infra.dal.database.dataobject.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 角色
 * @TableName sys_role
 */
@TableName(value ="sys_role")
@Data
public class SysRoleDO implements Serializable {
    /**
     * 角色id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 角色名称
     */
    @TableField(value = "role_name")
    private String role_name;

    /**
     * 描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 状态(0:禁止,1:正常)
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime create_time;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private LocalDateTime update_time;

    /**
     * 实现逻辑删除（0:不可用 1:可用）
     */
    @TableField(value = "is_deleted")
    private Integer is_deleted;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}