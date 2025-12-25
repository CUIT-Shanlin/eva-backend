package edu.cuit.bc.iam.application.contract.api.user;

import edu.cuit.bc.iam.application.contract.dto.clientobject.user.GenericMenuSectionCO;
import edu.cuit.bc.iam.application.contract.dto.clientobject.user.MenuCO;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.NewMenuCmd;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.UpdateMenuCmd;
import edu.cuit.client.dto.query.condition.MenuConditionalQuery;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 权限菜单相关用户接口
 */
public interface IMenuService {

    /**
     * 获取树形菜单数据
     * @param query 菜单条件查询模型
     */
    List<MenuCO> mainMenu(MenuConditionalQuery query);

    /**
     * 获取一个菜单信息
     * @param id 菜单id
     */
    GenericMenuSectionCO one(Integer id);

    /**
     * 修改菜单信息
     * @param updateMenuCmd 修改菜单模型
     */
    void update(UpdateMenuCmd updateMenuCmd);

    /**
     * 创建菜单
     * @param newMenuCmd 创建菜单模型
     */
    void create(NewMenuCmd newMenuCmd);

    /**
     * 删除菜单
     * @param menuId 菜单id
     */
    void delete(Integer menuId);

    /**
     * 批量删除菜单
     * @param ids 待删除菜单id列表
     */
    void multipleDelete(List<Integer> ids);

}
