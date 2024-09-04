package edu.cuit.adapter.controller.user.query;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.user.RoleInfoCO;
import edu.cuit.client.dto.clientobject.user.SimpleRoleInfoCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色查询相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class RoleQueryController {

    /**
     * 分页获取角色信息
     * @param pagingQuery 分页查询模型
     */
    @PostMapping("/roles")
    @SaCheckPermission("system.role.query")
    public CommonResult<PaginationQueryResultCO<RoleInfoCO>> page(@RequestBody @Valid PagingQuery pagingQuery) {
        return null;
    }

    /**
     * 获取一个角色信息
     * @param id 角色id
     */
    @GetMapping("/role")
    @SaCheckPermission("system.role.query")
    public CommonResult<RoleInfoCO> one(@RequestParam("id") Integer id) {
        return null;
    }

    /**
     * 所有角色信息
     */
    @GetMapping("/role/all")
    @SaCheckPermission("system.role.query")
    public CommonResult<List<SimpleRoleInfoCO>> all() {
        return null;
    }

    /**
     * 获取角色所拥有的菜单id
     * @param roleId 角色id
     */
    @GetMapping("/menu/idList/{roleId}")
    @SaCheckPermission("system.menu.query")
    public CommonResult<List<Integer>> menus(@PathVariable("roleId") Integer roleId) {
        return null;
    }


}
