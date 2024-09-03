package edu.cuit.adapter.controller.user.update;

import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
public class UserUpdateController {
    @DeleteMapping("/user")
    public CommonResult<Object> deleteUserById(@RequestParam("userId") Integer userId){
        return null;
    }
}
