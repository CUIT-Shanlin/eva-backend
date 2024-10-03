package edu.cuit.domain.gateway.user;

import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 用户相关数据门户接口
 */
@Component
public interface UserQueryGateway {

    /**
     * 通过用户ID查询用户
     * @param id 用户ID
     * @return UserEntity
     */
    Optional<UserEntity> findById(Integer id);

    /**
     * 通过用户名查询用户
     * @param username 用户名
     * @return UserEntity
     */
    Optional<UserEntity> findByUsername(String username);

    /**
     * 分页获取用户信息
     * @param query 查询对象
     * @return 数据
     */
    PaginationResultEntity<UserEntity> page(PagingQuery<GenericConditionalQuery> query);

    /**
     * 获取所有用户
     * @return 极简响应模型
     */
    List<SimpleResultCO> allUser();

    /**
     * 获取用户角色列表
     * @param userId 用户id
     */
    List<Integer> getUserRoleIds(Integer userId);

    /**
     * 判断用户名是否存在
     * @param username 用户名
     * @return 是否存在
     */
    Boolean isUsernameExist(String username);
}
