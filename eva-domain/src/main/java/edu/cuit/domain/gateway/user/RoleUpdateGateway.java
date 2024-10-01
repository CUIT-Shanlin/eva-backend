package edu.cuit.domain.gateway.user;

import edu.cuit.client.dto.cmd.user.NewRoleCmd;
import edu.cuit.client.dto.cmd.user.UpdateRoleCmd;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 角色修改数据门面
 */
@Component
public interface RoleUpdateGateway {

    /**
     * 修改角色信息
     * @param cmd 请求对象
     */
    void updateRoleInfo(UpdateRoleCmd cmd);

    /**
     * 修改角色状态
     * @param status 状态，1为禁止，0为正常
     */
    void updateRoleStatus(Integer roleId,Integer status);

    /**
     * 删除角色
     * @param roleId 角色id
     */
    void deleteRole(Integer roleId);

    /**
     * 批量删除角色
     * @param ids 角色id数组
     */
    void deleteMultipleRole(List<Integer> ids);

    /**
     * 分配权限
     * @param roleId 角色id
     * @param menuIds 菜单id数组
     */
    void assignPerms(Integer roleId,List<Integer> menuIds);

    /**
     * 创建角色
     * @param cmd 请求对象
     */
    void createRole(NewRoleCmd cmd);
}
