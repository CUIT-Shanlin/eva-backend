package edu.cuit.domain.gateway.user;

import edu.cuit.client.dto.cmd.user.NewUserCmd;
import edu.cuit.client.dto.cmd.user.UpdateUserCmd;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户修改数据门面
 */
@Component
public interface UserUpdateGateway {

    /**
     * 修改用户信息
     * @param cmd cmd对象
     */
    void updateInfo(UpdateUserCmd cmd);

    /**
     * 更改用户状态
     * @param userId 用户id
     * @param status 状态值（1为禁止，0为正常）
     */
    void updateStatus(Integer userId,Integer status);

    /**
     * 删除用户信息
     * @param userId 用户id
     */
    void deleteUser(Integer userId);

    /**
     * 分配角色
     * @param userId 用户id
     * @param roleId 角色id列表
     */
    void assignRole(Integer userId, List<Integer> roleId);

    /**
     * 创建用户
     * @param cmd 创建用户cmd
     */
    void createUser(NewUserCmd cmd);

}
