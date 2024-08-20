package edu.cuit.client.dto.cmd;

import edu.cuit.client.dto.clientobject.user.RoleDetailCO;
import edu.cuit.client.dto.clientobject.user.UserDetailCO;

import java.util.List;

/**
 *

 新建/修改用户模型(新建/修改用户模型)
 */
public class UpdateUserCmd extends UserDetailCO {
    //修改的用户密码
    private String password;
    //修改的用户头像
    private String avatar;
    //修改的用户性别
    private Integer sex;
    /**
     * 角色列表
     */
    private List<RoleDetailCO> roleList;


}
