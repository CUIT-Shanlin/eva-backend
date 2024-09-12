package edu.cuit.domain.gateway.user;

import edu.cuit.domain.entity.user.biz.UserEntity;
import org.springframework.stereotype.Component;

/**
 * 用户相关数据门户接口
 */
@Component
public interface UserGateway {

    /**
     * 通过用户ID查询用户
     * @param id 用户ID
     * @return UserEntity
     */
    UserEntity findById(Integer id);

    /**
     * 通过用户名查询用户
     * @param username 用户名
     * @return UserEntity
     */
    UserEntity findByUsername(String username);

//    PaginationEntity<UserEntity> list
}
