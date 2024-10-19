package edu.cuit.adapter.controller.user.query;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.api.eva.IEvaStatisticsService;
import edu.cuit.client.api.user.IUserService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.eva.UserSingleCourseScoreCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserResultCO;
import edu.cuit.client.dto.clientobject.user.UserInfoCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.client.dto.query.condition.UnqualifiedUserConditionalQuery;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
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

    private final IUserService userService;
    private final IEvaStatisticsService evaStatisticsService;

    /**
     * 一个用户信息
     * @param id 用户id
     */
    @GetMapping("/user/{id}")
    @SaCheckPermission("system.user.query")
    public CommonResult<UserInfoCO> oneUserInfo(@PathVariable("id") Integer id) {
        return CommonResult.success(userService.getOneUserInfo(id));
    }

    /**
     * 分页用户信息
     * @param query 查询dto
     */
    @PostMapping("/users")
    @SaCheckPermission("system.user.query")
    public CommonResult<PaginationQueryResultCO<UserInfoCO>> pageUserInfo(
            @RequestBody @Valid PagingQuery<GenericConditionalQuery> query) {
        return CommonResult.success(userService.pageUserInfo(query));
    }

    /**
     * 分页获取未达标用户
     * @param type 0：获取评教未达标的用户、1：获取被评教次数未达标的用户
     * @param target 评教或被评教的目标 数目，大于等于该数目则达标，小于则未达标
     * @param query 查询对象
     */
    @PostMapping("/users/unqualified/{type}/{target}")
    @SaCheckPermission("system.user.query")
    public CommonResult<PaginationQueryResultCO<UnqualifiedUserInfoCO>> pageUnqualifiedUser(
            @PathVariable("type") Integer type,
            @PathVariable("target") Integer target,
            @RequestBody @Valid PagingQuery<UnqualifiedUserConditionalQuery> query,
            @RequestParam(value = "semId",required = false) Integer semId) {
        return CommonResult.success(evaStatisticsService.pageUnqualifiedUser(type,target,query));
    }

    /**
     * 获取指定数目未达标的用户信息
     * @param type 0：获取 评教 未达标的用户、1：获取 被评教 次数未达标的用户
     * @param num 加载前几个用户数据
     * @param target 评教或被评教的目标 数目，大于等于该数目则达标，小于则未达标
     * @param semId 学期id
     */
    @GetMapping("/users/unqualified/{type}/{num}/{target}")
    @SaCheckPermission("system.user.query")
    public CommonResult<UnqualifiedUserResultCO> getTargetAmountUnqualifiedUser(@PathVariable("type") Integer type,
                                                                         @PathVariable("num") Integer num,
                                                                         @PathVariable("target") Integer target,
                                                                         @RequestParam(value = "semId",required = false) Integer semId) {
        return CommonResult.success(evaStatisticsService.getTargetAmountUnqualifiedUser(type, num, target));
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
            @RequestParam(value = "semId",required = false) Integer semId) {
        return CommonResult.success(userService.getOneUserScore(userId,semId));
    }

    /**
     * 所有用户的信息
     */
    @GetMapping("/users/all")
    @SaCheckPermission("system.user.list")
    public CommonResult<List<SimpleResultCO>> allUserInfo(){
        return CommonResult.success(userService.getAllUserInfo());
    }

    /**
     * 用户自己的信息
     */
    @GetMapping("/user/info")
    @SaCheckLogin
    public CommonResult<UserInfoCO> selfUserInfo(){
        return CommonResult.success(userService.getSelfUserInfo());
    }

    /**
     * 用户头像
     * @param id 用户id
     * @return 响应对象，包含图片二进制数据，响应体content-type为image/jpeg
     */
    @GetMapping(value = "/user/avatar/{id}",produces = {"image/jpeg"})
    public ResponseEntity<byte[]> userAvatar(@PathVariable("id") Integer id) {
        byte[] userAvatarData = userService.getUserAvatar(id);
        HttpStatusCode status = userAvatarData.length == 0 ? HttpStatus.NOT_FOUND : HttpStatus.OK;
        return new ResponseEntity<>(userAvatarData,status);
    }

    /**
     * 检查用户名是否存在
     * @param username 用户名
     */
    @GetMapping("/username/exist")
    @SaCheckPermission("system.user.isExist")
    public CommonResult<Boolean> isExist(@RequestParam("username") String username) {
        return CommonResult.success(userService.isUsernameExist(username));
    }

}
