package edu.cuit.adapter.controller.user.query;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.api.user.IMenuService;
import edu.cuit.client.dto.clientobject.user.GenericMenuSectionCO;
import edu.cuit.client.dto.clientobject.user.MenuCO;
import edu.cuit.client.dto.query.condition.MenuConditionalQuery;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限菜单查询相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class MenuQueryController {

    private final IMenuService menuService;

    /**
     * 获取树形菜单数据
     * @param query 菜单条件查询模型
     */
    @PostMapping("/menus/tree")
    @SaCheckPermission("system.menu.tree")
    public CommonResult<List<MenuCO>> mainMenu(@RequestBody @Valid MenuConditionalQuery query) {
        return CommonResult.success(menuService.mainMenu(query));
    }

    /**
     * 获取一个菜单信息
     * @param id 菜单id
     */
    @GetMapping("/menu")
    @SaCheckPermission("system.menu.query")
    public CommonResult<GenericMenuSectionCO> one(@RequestParam("id") Integer id) {
        return CommonResult.success(menuService.one(id));
    }


}
