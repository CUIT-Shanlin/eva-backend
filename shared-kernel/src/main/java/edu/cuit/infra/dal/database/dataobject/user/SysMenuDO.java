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
 * 菜单权限表
 * @TableName sys_menu
 */
@TableName(value ="sys_menu")
@Data
public class SysMenuDO implements Serializable {
    /**
     * 菜单权限id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 父菜单
     */
    @TableField(value = "parent_id")
    private Integer parentId;

    /**
     * 名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 类型(0:目录,1:菜单,2:按钮)
     */
    @TableField(value = "type")
    private Integer type;

    /**
     * 路由地址
     */
    @TableField(value = "path")
    private String path;

    /**
     * 组件路径
     */
    @TableField(value = "component")
    private String component;

    /**
     * 权限标识
     */
    @TableField(value = "perms")
    private String perms;

    /**
     * 图标unicode码
     */
    @TableField(value = "icon")
    private String icon;

    /**
     * 状态(0:禁止,1:正常)
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    /**
     * 实现逻辑删除（0:不可用 1:可用）
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}