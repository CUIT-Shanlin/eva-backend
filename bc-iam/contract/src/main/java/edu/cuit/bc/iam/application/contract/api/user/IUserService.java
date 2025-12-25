package edu.cuit.bc.iam.application.contract.api.user;

import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.eva.UserSingleCourseScoreCO;
import edu.cuit.bc.iam.application.contract.dto.clientobject.user.UserInfoCO;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.AssignRoleCmd;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.NewUserCmd;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.UpdatePasswordCmd;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.UpdateUserCmd;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;

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
     * 通过用户名获取用户ID
     * 未找到则抛出异常
     * @param username 用户名
     */
    Integer getIdByUsername(String username);

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
     * 修改用户自己的信息
     * @param cmd 修改用户模型
     */
    void updateOwnInfo(UpdateUserCmd cmd);

    /**
     * 更改用户密码
     * @param userId 用户id
     * @param newPassword 新密码
     */
    void changePassword(Integer userId, UpdatePasswordCmd cmd);

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

    /**
     * 同步ldap数据
     * 遇到username相同用户则跳过
     */
    void syncLdap();

}
