package edu.cuit.client.dto.clientobject.user;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 用户信息（包含路由数据等）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class UserInfoCO extends ClientObject {

    /**
     * 用户基本信息
     */
    private UserDetailCO info;

    /**
     * 路由列表
     */
    private List<RouterDetailCO> routerList;

    /**
     * 角色列表
     */
    private List<RoleDetailCO> roleList;

    /**
     * 按钮列表
     */
    private List<String> buttonList;

}
