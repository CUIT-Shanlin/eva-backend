package edu.cuit.adapter.controller.user.update;

import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户相关更新操作接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class UserUpdateController {

    /**
     * 删除用户
     * @param userId 用户id
     */
    @DeleteMapping("/user")
    public CommonResult<Void> deleteUserById(@RequestParam("userId") Integer userId){
        return null;
    }

    //TODO 其余接口

}
