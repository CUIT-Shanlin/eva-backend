package edu.cuit.adapter.controller.user;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.dto.clientobject.EvaMsgCO;
import edu.cuit.client.dto.cmd.SendWarningMsgCmd;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户消息相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/msg")
public class MessageController {

    /**
     * 向评教者发送警告
     * @param cmd 信息模型
     */
    @PostMapping("/tip")
    @SaCheckPermission("system.message.send")
    public CommonResult<Void> warning(@RequestBody @Valid SendWarningMsgCmd cmd) {
        return null;
    }

    /**
     * 获取当前用户的待办/通知
     * @param type 消息类型（-1或null：全部，0：待办，1：通知）
     */
    @GetMapping("/tips")
    @SaCheckLogin
    public CommonResult<List<EvaMsgCO>> currentUserMsg(@RequestParam("type") Integer type) {
        return null;
    }

}
