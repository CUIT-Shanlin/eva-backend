package edu.cuit.adapter.controller.user.update;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.api.user.IMenuService;
import edu.cuit.client.dto.cmd.user.NewMenuCmd;
import edu.cuit.client.dto.cmd.user.UpdateMenuCmd;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限菜单修改相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class MenuUpdateController {

    private final IMenuService menuService;

    /**
     * 修改菜单信息
     * @param updateMenuCmd 修改菜单模型
     */
    @PutMapping("/menu")
    @SaCheckPermission("system.menu.update")
    public CommonResult<Void> update(@RequestBody @Valid UpdateMenuCmd updateMenuCmd) {
        menuService.update(updateMenuCmd);
        return CommonResult.success();
    }

    /**
     * 创建菜单
     * @param newMenuCmd 创建菜单模型
     */
    @PostMapping("/menu")
    @SaCheckPermission("system.menu.add")
    public CommonResult<Void> create(@RequestBody @Valid NewMenuCmd newMenuCmd) {
        menuService.create(newMenuCmd);
        return CommonResult.success();
    }

    /**
     * 删除菜单
     * @param menuId 菜单id
     */
    @DeleteMapping("/menu/{menuId}")
    @SaCheckPermission("system.menu.delete")
    public CommonResult<Void> delete(@PathVariable("menuId") Integer menuId) {
        menuService.delete(menuId);
        return CommonResult.success();
    }

    /**
     * 批量删除菜单
     * @param ids 待删除菜单id列表
     */
    @DeleteMapping("/menus")
    @SaCheckPermission("system.menu.delete")
    public CommonResult<Void> multipleDelete(@RequestBody List<Integer> ids) {
        menuService.multipleDelete(ids);
        return CommonResult.success();
    }

}
