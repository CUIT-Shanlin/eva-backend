package edu.cuit.adapter.controller.user.query;

import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.user.UserInfoCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户信息查询相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class UserQueryController {

    /**
     * 一个用户信息
     * @param id 用户id
     * @param semId 学期id
     */
    @GetMapping("/user/{id}")
    public CommonResult<UserInfoCO> oneUserInfo(
            @PathVariable("id") Integer id,
            @RequestParam("semId") Integer semId) {
        return null;
    }

    /**
     * 分页用户信息
     * @param semId 学期id
     * @param query 查询dto
     */
    @PostMapping("/users")
    public CommonResult<PaginationQueryResultCO<UserInfoCO>> pageUserInfo(
            @RequestParam("semId") Integer semId,
            @RequestBody PagingQuery query) {
        return null;
    }

    //TODO 其余接口
}
