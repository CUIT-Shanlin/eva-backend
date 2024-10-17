package edu.cuit.adapter.controller.user.update;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.dto.cmd.user.AssignRoleCmd;
import edu.cuit.client.dto.cmd.user.NewUserCmd;
import edu.cuit.client.dto.cmd.user.UpdatePasswordCmd;
import edu.cuit.client.dto.cmd.user.UpdateUserCmd;
import edu.cuit.client.validator.status.ValidStatus;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/user")
public class UserUpdateController {

    /**
     * 修改用户信息
     * @param isUpdatePwd 是否需要修改密码
     * @param cmd 修改用户模型
     */
    @PutMapping("/{isUpdatePwd}")
    @SaCheckPermission("system.user.update")
    public CommonResult<Void> updateInfo(@PathVariable("isUpdatePwd") Boolean isUpdatePwd,
                                     @RequestBody @Valid UpdateUserCmd cmd) {
        return null;
    }

    /**
     * 修改用户自己的信息
     * @param cmd 修改用户模型
     */
    @PutMapping("/info")
    @SaCheckLogin
    public CommonResult<Void> updateOwnInfo(@RequestBody @Valid UpdateUserCmd cmd) {
        return null;
    }

    /**
     * 修改用户自己的密码
     * 如果传入的新密码和旧密码重复或者旧密码错误直接异常返回
     * @param cmd 修改密码模型
     */
    @PutMapping("/user/password")
    @SaCheckLogin
    public CommonResult<Void> updatePassword(@RequestBody @Valid UpdatePasswordCmd cmd) {
        return null;
    }

    /**
     * 修改用户状态
     * @param userId 用户id
     * @param status 状态 1为禁止，0为正常
     */
    @PutMapping("/status/{userId}/{status}")
    @SaCheckPermission("system.user.update")
    public CommonResult<Void> updateStatus(@PathVariable("userId") Integer userId,
                                           @PathVariable("status") @ValidStatus(message = "状态只能为0或1") Integer status) {
        return null;
    }

    /**
     * 删除用户
     * @param userId 用户id
     */
    @DeleteMapping
    @SaCheckPermission("system.user.delete")
    public CommonResult<Void> delete(@RequestParam("userId") Integer userId) {
        return null;
    }

    /**
     * 分配角色
     * @param cmd 分配角色模型
     */
    @PutMapping("/roles")
    @SaCheckPermission("system.user.assignRole")
    public CommonResult<Void> assignRole(@RequestBody @Valid AssignRoleCmd cmd) {
        return null;
    }

    /**
     * 新建用户
     * @param cmd 新建用户模型
     */
    @PostMapping("/user")
    @SaCheckPermission("system.user.add")
    public CommonResult<Void> create(@RequestBody @Valid NewUserCmd cmd) {
        return null;
    }

    /**
     * 修改用户自己的头像
     * @param avatarFile 头像文件
     */
    @SaCheckLogin
    @PostMapping("/user/info/avatar")
    public CommonResult<Void> uploadAvatar(@RequestParam("avatarFile") @NotNull(message = "头像文件不能为空") MultipartFile avatarFile) {
        return null;
    }

}
