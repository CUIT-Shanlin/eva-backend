package edu.cuit.bc.iam.application.contract.api.user;

import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.bc.iam.application.contract.dto.clientobject.user.RoleInfoCO;
import edu.cuit.bc.iam.application.contract.dto.clientobject.user.SimpleRoleInfoCO;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.AssignPermCmd;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.NewRoleCmd;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.UpdateRoleCmd;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;

import java.util.List;

/**
 * 角色相关业务接口
 */
public interface IRoleService {

    /**
     * 分页获取角色信息
     * @param pagingQuery 分页查询模型
     */
    PaginationQueryResultCO<RoleInfoCO> page(PagingQuery<GenericConditionalQuery> pagingQuery);

    /**
     * 获取一个角色信息
     * @param id 角色id
     */
    RoleInfoCO one(Integer id);

    /**
     * 所有角色信息
     */
    List<SimpleRoleInfoCO> all();

    /**
     * 获取角色所拥有的菜单id
     * @param roleId 角色id
     */
    List<Integer> roleMenus(Integer roleId);

    /**
     * 修改角色信息
     * @param updateRoleCmd 修改角色模型
     */
    void updateInfo(UpdateRoleCmd updateRoleCmd);

    /**
     * 修改角色状态
     * @param roleId 角色id
     * @param status 状态，0正常，1禁止
     */
    void updateStatus(Integer roleId,Integer status);

    /**
     * 分配权限
     * @param assignPermCmd 分配权限模型
     */
    void assignPerm(AssignPermCmd assignPermCmd);

    /**
     * 新建角色
     * @param newRoleCmd 新建角色模型
     */
    void create(NewRoleCmd newRoleCmd);

    /**
     * 删除角色
     * @param roleId 角色id
     */
    void delete(Integer roleId);

    /**
     * 批量删除角色
     * @param ids 角色id
     */
    void multipleDelete(List<Integer> ids);

}
