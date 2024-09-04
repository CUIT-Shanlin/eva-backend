package edu.cuit.adapter.controller.user.update;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.dto.cmd.user.UpdateRoleCmd;
import edu.cuit.common.validator.status.ValidStatus;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 角色修改相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/role")
public class RoleUpdateController {

    /**
     * 修改角色信息
     * @param updateRoleCmd 修改角色模型
     */
    @PutMapping
    @SaCheckPermission("system.role.update")
    public CommonResult<Void> updateInfo(@RequestBody @Valid UpdateRoleCmd updateRoleCmd) {
        return null;
    }

    /**
     * 修改角色状态
     * @param roleId 角色id
     * @param status 状态，0正常，1禁止
     */
    @PutMapping("/status/{roleId}/{status}")
    @SaCheckPermission("system.role.update")
    public CommonResult<Void> updateStatus(@PathVariable("roleId") Integer roleId,
                                           @PathVariable("status") @ValidStatus Integer status) {
        return null;
    }

}
