package edu.cuit.adapter.controller.user.update;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.cola.exception.SysException;
import edu.cuit.client.api.user.IUserService;
import edu.cuit.client.dto.cmd.user.AssignRoleCmd;
import edu.cuit.client.dto.cmd.user.NewUserCmd;
import edu.cuit.client.dto.cmd.user.UpdatePasswordCmd;
import edu.cuit.client.dto.cmd.user.UpdateUserCmd;
import edu.cuit.client.validator.status.ValidStatus;
import edu.cuit.common.enums.LogModule;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import edu.cuit.zhuyimeng.framework.logging.aspect.annotation.OperateLog;
import edu.cuit.zhuyimeng.framework.logging.aspect.enums.OperateLogType;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/user")
@Slf4j
public class UserUpdateController {

    private final IUserService userService;

    /**
     * 修改用户信息
     * @param isUpdatePwd 是否需要修改密码
     * @param cmd 修改用户模型
     */
    @PutMapping("/{isUpdatePwd}")
    @SaCheckPermission("system.user.update")
    @OperateLog(module = LogModule.USER,type = OperateLogType.UPDATE)
    public CommonResult<Void> updateInfo(@PathVariable("isUpdatePwd") Boolean isUpdatePwd,
                                     @RequestBody @Valid UpdateUserCmd cmd) {
        userService.updateInfo(isUpdatePwd,cmd);
        return CommonResult.success();
    }

    /**
     * 修改用户自己的信息
     * @param cmd 修改用户模型
     */
    @PutMapping("/info")
    @SaCheckLogin
    public CommonResult<Void> updateOwnInfo(@RequestBody @Valid UpdateUserCmd cmd) {
        userService.updateOwnInfo(cmd);
        return CommonResult.success();
    }

    /**
     * 修改用户自己的密码
     * 如果传入的新密码和旧密码重复或者旧密码错误直接异常返回
     * @param cmd 修改密码模型
     */
    @PutMapping("/password")
    @SaCheckLogin
    public CommonResult<Void> updatePassword(@RequestBody @Valid UpdatePasswordCmd cmd) {
        userService.changePassword(userService.getIdByUsername(((String) StpUtil.getLoginId())), cmd);
        return CommonResult.success();
    }

    /**
     * 修改用户状态
     * @param userId 用户id
     * @param status 状态 1为禁止，0为正常
     */
    @PutMapping("/status/{userId}/{status}")
    @SaCheckPermission("system.user.update")
    @OperateLog(module = LogModule.USER,type = OperateLogType.UPDATE)
    public CommonResult<Void> updateStatus(@PathVariable("userId") Integer userId,
                                           @PathVariable("status") @ValidStatus(message = "状态只能为0或1") Integer status) {
        userService.updateStatus(userId,status);
        return CommonResult.success();
    }

    /**
     * 删除用户
     * @param userId 用户id
     */
    @DeleteMapping
    @SaCheckPermission("system.user.delete")
    @OperateLog(module = LogModule.USER,type = OperateLogType.DELETE)
    public CommonResult<Void> delete(@RequestParam("userId") Integer userId) {
        userService.delete(userId);
        return CommonResult.success();
    }

    /**
     * 分配角色
     * @param cmd 分配角色模型
     */
    @PutMapping("/roles")
    @SaCheckPermission("system.user.assignRole")
    @OperateLog(module = LogModule.USER,type = OperateLogType.UPDATE)
    public CommonResult<Void> assignRole(@RequestBody @Valid AssignRoleCmd cmd) {
        userService.assignRole(cmd);
        return CommonResult.success();
    }

    /**
     * 新建用户
     * @param cmd 新建用户模型
     */
    @PostMapping
    @SaCheckPermission("system.user.add")
    @OperateLog(module = LogModule.USER,type = OperateLogType.CREATE)
    public CommonResult<Void> create(@RequestBody @Valid NewUserCmd cmd) {
        userService.create(cmd);
        LogUtils.logContent(cmd.getName() + " 用户");
        return CommonResult.success();
    }

    /**
     * 修改用户自己的头像
     * @param avatarFile 头像文件
     */
    @SaCheckLogin
    @PostMapping("/info/avatar")
    public CommonResult<Void> uploadAvatar(@RequestParam("avatarFile") @NotNull(message = "头像文件不能为空") MultipartFile avatarFile) {
        try {
            userService.uploadUserAvatar(userService.getIdByUsername(((String) StpUtil.getLoginId())), avatarFile.getInputStream());
        } catch (IOException e) {
            SysException ex = new SysException("处理上传图片失败，请联系管理员");
            log.error("发生系统异常",ex);
            throw ex;
        }
        return CommonResult.success();
    }

    /**
     * 同步Ldap用户数据
     */
    @PostMapping("/sync")
    @SaCheckPermission("system.user.sync")
    @OperateLog(module = LogModule.USER,type = OperateLogType.CREATE)
    public CommonResult<Void> syncLdapUser() {
        userService.syncLdap();
        LogUtils.logContent(" 同步LDAP用户任务");
        return CommonResult.success();
    }

}
