package edu.cuit.adapter.controller.user.query;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.eva.UserSingleCourseScoreCO;
import edu.cuit.client.dto.clientobject.user.UserInfoCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @SaCheckPermission("system.user.query")
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
    @SaCheckPermission("system.user.query")
    public CommonResult<PaginationQueryResultCO<UserInfoCO>> pageUserInfo(
            @RequestParam("semId") Integer semId,
            @RequestBody PagingQuery query) {
        return null;
    }

    /**
     * 用户的各个课程的评分
     * @param userId 用户id
     * @param semId 学期id
     */
    @GetMapping("/user/score/{userId}")
    @SaCheckPermission("system.user.score.query")
    public CommonResult<List<UserSingleCourseScoreCO>> oneUserScore(
            @PathVariable("userId") Integer userId,
            @RequestParam("semId") Integer semId) {
        return null;
    }

    /**
     * 所有用户的信息
     * @param semId 学期id
     */
    @GetMapping("/users/all")
    @SaCheckPermission("system.user.list")
    public CommonResult<SimpleResultCO> allUserInfo(@RequestParam("semId") Integer semId){
        return null;
    }

    /**
     * 用户自己的信息
     * @param semId 学期id
     */
    @GetMapping("/user/info")
    @SaCheckLogin
    public CommonResult<UserInfoCO> selfUserInfo(@RequestParam("semId") Integer semId){
        return null;
    }

    /**
     * 用户头像
     * @param id 用户id
     */
    @GetMapping("/user/avatar/{id}")
    public ResponseEntity<String> userAvatar(@PathVariable("id") Integer id){
        return null;
    }

}
