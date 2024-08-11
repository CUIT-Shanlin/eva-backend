package edu.cuit.domain.gateway.user;

import edu.cuit.domain.entity.user.LdapPersonEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 用户访问ldap 用户的接口
 */
public interface LdapPersonGateway {

    /**
     * 认证密码是否正确（采用bind dn的方式）
     * @param username 用户名
     * @param password 未加密的密码
     * @return 是否认证成功
     */
    boolean authenticate(String username,String password);

    /**
     * 根据用户名查询
     * @param username 用户名
     * @return Optional<LdapPersonEntity>
     */
    Optional<LdapPersonEntity> findByUsername(String username);

    /**
     * 新增用户
     * @param user LdapPersonEntity
     * @param password 未加密的密码
     * @deprecated 不建议使用，建议直接在lam中添加用户
     */
    @Deprecated
    void saveUser(LdapPersonEntity user,String password);
}
