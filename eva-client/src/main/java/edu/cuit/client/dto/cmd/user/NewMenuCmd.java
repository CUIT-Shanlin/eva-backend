package edu.cuit.client.dto.cmd.user;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.validator.status.ValidStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 创建菜单模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class NewMenuCmd extends ClientObject {

    /**
     * 名称
     */
    private String name;

    /**
     * 类型(0:目录,1:菜单,2:按钮)
     */
    @NotNull(message = "菜单类型不能为空")
    @ValidStatus(value = {0,1,2},message = "菜单类型无效，0:目录,1:菜单,2:按钮)")
    private Integer type;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 权限标识
     */
    private String perms;

    /**
     * 图标unicode码
     */
    private String icon;

    /**
     * 状态(0:禁止,1:正常)
     */
    @ValidStatus(message = "菜单状态只能为0或1")
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 父菜单id
     */
    @NotNull(message = "父菜单id不能为空")
    private Integer parentId;

    /**
     * 父菜单名称
     */
    private String parentName;


}
