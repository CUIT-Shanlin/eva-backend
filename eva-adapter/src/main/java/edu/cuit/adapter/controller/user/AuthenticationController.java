package edu.cuit.adapter.controller.user;

import cn.dev33.satoken.annotation.SaCheckLogin;
import edu.cuit.client.dto.cmd.user.UserLoginCmd;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户认证相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class AuthenticationController {

    /**
     * 登录请求
     */
    @PostMapping("/login")
    public CommonResult<Pair<String,String>> login(@Valid @RequestBody UserLoginCmd loginCmd) {
        return null;
    }

    /**
     * 退出登录
     */
    @GetMapping("/logout")
    @SaCheckLogin //需要登录了才能退出登录
    public CommonResult<Void> logout() {
        return null;
    }

}
