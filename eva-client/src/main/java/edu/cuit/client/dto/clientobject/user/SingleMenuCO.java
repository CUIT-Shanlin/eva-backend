package edu.cuit.client.dto.clientobject.user;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 *单个菜单信息
 */

@Data
@Accessors(chain = true)
public class SingleMenuCO {
    //菜单权限id
    private Integer id;
    //名称
    private String name;
    //类型(0:目录,1:菜单,2:按钮)
    private Integer type;
    //路由地址
    private String path;
    //组件路径
    private String component;
    //权限标识
    private String perms;
    //图标unicode码
    private String icon;
    //状态(0:禁止,1:正常)
    private Integer status;
    //父菜单id
    private Integer parentId;
    //父菜单名称
    private String parentName;
    //创建时间
    private String createTime;
    //更新时间
    private String updateTime;


}
