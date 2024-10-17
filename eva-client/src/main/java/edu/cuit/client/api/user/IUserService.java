package edu.cuit.client.api.user;

import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.eva.UserSingleCourseScoreCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserResultCO;
import edu.cuit.client.dto.clientobject.user.UserInfoCO;
import edu.cuit.client.dto.cmd.user.AssignRoleCmd;
import edu.cuit.client.dto.cmd.user.NewUserCmd;
import edu.cuit.client.dto.cmd.user.UpdateUserCmd;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.client.dto.query.condition.UnqualifiedUserConditionalQuery;

import java.io.InputStream;
import java.util.List;

/**
 * 用户相关业务接口
 */
public interface IUserService {

    /**
     * 一个用户信息
     *
     * @param id 用户id
     */
    UserInfoCO getOneUserInfo(Integer id);

    /**
     * 分页用户信息
     *
     * @param query 查询dto
     */
    PaginationQueryResultCO<UserInfoCO> pageUserInfo(PagingQuery<GenericConditionalQuery> query);

    /**
     * 分页获取未达标用户
     *
     * @param type   0：获取评教未达标的用户、1：获取被评教次数未达标的用户
     * @param target 评教或被评教的目标 数目，大于等于该数目则达标，小于则未达标
     * @param query  查询对象
     */
    PaginationQueryResultCO<UnqualifiedUserInfoCO> pageUnqualifiedUser(Integer type,
                                                                   Integer target,
                                                                   PagingQuery<UnqualifiedUserConditionalQuery> query);

    /**
     * 获取指定数目未达标的用户信息
     *
     * @param type   0：获取 评教 未达标的用户、1：获取 被评教 次数未达标的用户
     * @param num    加载前几个用户数据
     * @param target 评教或被评教的目标 数目，大于等于该数目则达标，小于则未达标
     */
    UnqualifiedUserResultCO getTargetAmountUnqualifiedUser(Integer type, Integer num, Integer target);

    /**
     * 用户的各个课程的评分
     *
     * @param userId 用户id
     * @param semId  学期id
     */
    List<UserSingleCourseScoreCO> getOneUserScore(Integer userId,Integer semId);

    /**
     * 所有用户的信息
     */
    List<SimpleResultCO> getAllUserInfo();

    /**
     * 用户自己的信息
     */
    UserInfoCO getSelfUserInfo();

    /**
     * 用户头像
     * @param id 用户id
     * @return 图片二进制数据 格式：data:image/jpeg;base64,...
     */
    byte[] getUserAvatar(Integer id);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     */
    Boolean isUsernameExist(String username);

    /**
     * 修改用户头像
     * @param userId 用户id
     * @param inputStream 头像读取流
     */
    void uploadUserAvatar(Integer userId, InputStream inputStream);

    /**
     * 修改用户信息
     * @param isUpdatePwd 是否需要修改密码
     * @param cmd 修改用户模型
     */
    void updateInfo(Boolean isUpdatePwd,UpdateUserCmd cmd);

    /**
     * 修改用户状态
     * @param userId 用户id
     * @param status 状态 1为禁止，0为正常
     */
    void updateStatus(Integer userId,Integer status);

    /**
     * 删除用户
     * @param userId 用户id
     */
    void delete(Integer userId);

    /**
     * 分配角色
     * @param cmd 分配角色模型
     */
    void assignRole(AssignRoleCmd cmd);

    /**
     * 新建用户
     * @param cmd 新建用户模型
     */
    void create(NewUserCmd cmd);

}
