package edu.cuit.adapter.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.api.IMsgService;
import edu.cuit.client.dto.cmd.SendMessageCmd;
import edu.cuit.client.dto.data.msg.GenericRequestMsg;
import edu.cuit.client.dto.data.msg.GenericResponseMsg;
import edu.cuit.client.validator.status.ValidStatus;
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

    private final IMsgService msgService;

    /**
     * 获取当前用户自己的指定类型的所有消息
     * @param type 消息类型（0：待办，1：通知，2：提醒，3：警告；null或者负数：全部）
     * @param mode 确定是普通消息还是评教消息，0: 普通消息；1：评教消息，null或者负数：全部
     */
    @GetMapping("/tips/{type}/{mode}")
    @SaCheckLogin
    public CommonResult<List<GenericResponseMsg>> getUserTargetTypeMsg(@PathVariable("type") @ValidStatus(value = {0,1,2,3}, message = "消息类型只能为0,1,2,3,负数或空") Integer type,
                                                                       @PathVariable("mode") @ValidStatus(message = "mode只能是0,1,负数或空") Integer mode) {
        //TODO 调用课程信息
        return CommonResult.success(msgService.getUserTargetTypeMsg(type,mode,null));
    }

    /**
     * 获取用户自己指定数目的指定类型的消息
     * @param num 指定消息数目，（负数或者null：全部）
     * @param type 消息类型（0：待办，1：通知，2：提醒，3：警告；null或者负数：全部）
     */
    @GetMapping("/tips/myNum/{num}/{type}")
    @SaCheckLogin
    public CommonResult<List<GenericResponseMsg>> getUserTargetAmountAndTypeMsg(@PathVariable("num") Integer num,
                                                                                @PathVariable("type") @ValidStatus(value = {0,1,2,3}, message = "消息类型只能为0,1,2,3,负数或空") Integer type) {
        return CommonResult.success(msgService.getUserTargetAmountAndTypeMsg(num,type));
    }

    /**
     * 修改某条消息的显示状态
     * @param id 消息id
     * @param isDisplayed 待改成的显示状态，0：未显示过，1：已显示过
     */
    @PutMapping("/tip/isDisplayed")
    @SaCheckLogin
    public CommonResult<Void> updateMsgDisplay(@RequestParam("id") Integer id,
                                               @RequestParam("isDisplayed") @ValidStatus(message = "显示状态只能为0或1") Integer isDisplayed) {
        msgService.updateMsgDisplay(id,isDisplayed);
        return CommonResult.success();
    }

    /**
     * 修改某条消息的已读状态
     * @param id 消息id
     * @param isRead 待改成的已读状态，0：未读，1：已读
     */
    @PutMapping("/tip/isRead")
    @SaCheckLogin
    public CommonResult<Void> updateMsgRead(@RequestParam("id") Integer id,
                                            @RequestParam("isRead") @ValidStatus(message = "已读状态只能为0或1") Integer isRead) {
        msgService.updateMsgRead(id,isRead);
        return CommonResult.success();
    }

    /**
     * 批量修改某种性质的消息的已读状态，（注：改为已读的同时，也要改为已显示）
     * @param mode 确定待批量修改的是普通消息还是评教消息，0: 普通消息；1：评教消息
     */
    @PutMapping("/tips/{mode}}")
    @SaCheckLogin
    public CommonResult<Void> updateMultipleMsgRead(@PathVariable("mode") @ValidStatus(message = "mode只能为0或1") Integer mode) {
        msgService.updateMultipleMsgRead(mode);
        return CommonResult.success();
    }

    /**
     * 发送消息接口，主要用于管理员向用户发消息
     * @param msg 消息对象
     */
    @PostMapping("/tips/send")
    @SaCheckPermission("msg.tips.send")
    public CommonResult<Void> sendMessage(@RequestBody @Valid SendMessageCmd msg) {
        msgService.handleUserSendMessage(msg);
        return CommonResult.success();
    }
}
