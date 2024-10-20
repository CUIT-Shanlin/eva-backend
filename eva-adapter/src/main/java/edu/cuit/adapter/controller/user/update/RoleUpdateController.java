package edu.cuit.adapter.controller.user.update;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.api.user.IRoleService;
import edu.cuit.client.dto.cmd.user.AssignPermCmd;
import edu.cuit.client.dto.cmd.user.NewRoleCmd;
import edu.cuit.client.dto.cmd.user.UpdateRoleCmd;
import edu.cuit.client.validator.status.ValidStatus;
import edu.cuit.common.enums.LogModule;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import edu.cuit.zhuyimeng.framework.logging.aspect.annotation.OperateLog;
import edu.cuit.zhuyimeng.framework.logging.aspect.enums.OperateLogType;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色修改相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class RoleUpdateController {

    private final IRoleService roleService;

    /**
     * 修改角色信息
     * @param updateRoleCmd 修改角色模型
     */
    @PutMapping("/role")
    @SaCheckPermission("system.role.update")
    @OperateLog(module = LogModule.ROLE,type = OperateLogType.UPDATE)
    public CommonResult<Void> updateInfo(@RequestBody @Valid UpdateRoleCmd updateRoleCmd) {
        roleService.updateInfo(updateRoleCmd);
        return CommonResult.success();
    }

    /**
     * 修改角色状态
     * @param roleId 角色id
     * @param status 状态，0正常，1禁止
     */
    @PutMapping("/role/status/{roleId}/{status}")
    @SaCheckPermission("system.role.update")
    @OperateLog(module = LogModule.ROLE,type = OperateLogType.UPDATE)
    public CommonResult<Void> updateStatus(@PathVariable("roleId") Integer roleId,
                                           @PathVariable("status") @ValidStatus Integer status) {
        roleService.updateStatus(roleId,status);
        return CommonResult.success();
    }

    /**
     * 分配权限
     * @param assignPermCmd 分配权限模型
     */
    @PutMapping("/role/auth")
    @SaCheckPermission("system.role.assignPerm")
    @OperateLog(module = LogModule.ROLE,type = OperateLogType.UPDATE)
    public CommonResult<Void> assignPerm(@RequestBody @Valid AssignPermCmd assignPermCmd) {
        roleService.assignPerm(assignPermCmd);
        return CommonResult.success();
    }

    /**
     * 新建角色
     * @param newRoleCmd 新建角色模型
     */
    @PostMapping("/role")
    @SaCheckPermission("system.role.add")
    @OperateLog(module = LogModule.ROLE,type = OperateLogType.CREATE)
    public CommonResult<Void> create(@RequestBody @Valid NewRoleCmd newRoleCmd) {
        roleService.create(newRoleCmd);
        LogUtils.logContent(newRoleCmd.getRoleName() + "角色");
        return CommonResult.success();
    }

    /**
     * 删除角色
     * @param roleId 角色id
     */
    @DeleteMapping("/role")
    @SaCheckPermission("system.role.delete")
    @OperateLog(module = LogModule.ROLE,type = OperateLogType.DELETE)
    public CommonResult<Void> delete(@RequestParam("roleId") Integer roleId) {
        roleService.delete(roleId);
        return CommonResult.success();
    }

    /**
     * 批量删除角色
     * @param ids 角色id
     */
    @DeleteMapping("/roles")
    @SaCheckPermission("system.role.delete")
    @OperateLog(module = LogModule.ROLE,type = OperateLogType.DELETE)
    public CommonResult<Void> multipleDelete(@RequestBody List<Integer> ids) {
        roleService.multipleDelete(ids);
        LogUtils.logContent("ID为 " + ids + " 的角色");
        return CommonResult.success();
    }

}
