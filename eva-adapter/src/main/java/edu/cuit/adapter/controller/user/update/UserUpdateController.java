package edu.cuit.adapter.controller.user.update;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.dto.clientobject.user.UserInfoCO;
import edu.cuit.client.dto.cmd.user.AssignRoleCmd;
import edu.cuit.client.dto.cmd.user.NewUserCmd;
import edu.cuit.client.dto.cmd.user.UpdateUserCmd;
import edu.cuit.common.validator.status.ValidStatus;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户相关更新操作接口
 */
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/user")
public class UserUpdateController {

    /**
     * 删除用户
     * @param userId 用户id
     */
    @DeleteMapping
    @SaCheckPermission("system.user.delete")
    public CommonResult<Void> deleteUserById(@RequestParam("userId") Integer userId){
        return null;
    }

    /**
     * 修改用户信息
     * @param isUpdatePwd 是否更新密码
     */
    @PutMapping("/{isUpdatePwd}")
    @SaCheckPermission("system.user.update")
    public CommonResult<Void> updateInfo(@PathVariable("isUpdatePwd") Boolean isUpdatePwd, @Valid @RequestBody UpdateUserCmd updateUserCmd) {
        return null;
    }

    /**
     * 修改用户状态
     * @param userId 用户id
     * @param status 状态
     */
    @PutMapping("/status/{userId}/{status}")
    @SaCheckPermission("system.user.update")
    public CommonResult<Void> updateStatus(@PathVariable("userId") Integer userId,
                                           @PathVariable("status") @ValidStatus Integer status) {
        return null;
    }

    /**
     * 分配角色
     * @param assignRoleCmd 分配模型
     */
    @PutMapping("/roles")
    @SaCheckPermission("system.user.assignRole")
    public CommonResult<Void> assignRole(@RequestBody @Valid AssignRoleCmd assignRoleCmd) {
        return null;
    }

    /**
     * 新建用户
     * @param newUserCmd 新建用户模型
     */
    @PostMapping
    @SaCheckPermission("system.user.add")
    public CommonResult<Void> create(@RequestBody @Valid NewUserCmd newUserCmd) {
        return null;
    }

}
