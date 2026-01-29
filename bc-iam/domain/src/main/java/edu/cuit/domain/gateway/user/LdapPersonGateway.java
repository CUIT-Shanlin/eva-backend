package edu.cuit.domain.gateway.user;

import edu.cuit.domain.entity.user.LdapPersonEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 用户访问ldap 用户的数据门户接口
 */
@Component
public interface LdapPersonGateway {

    //测试

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
     * 查询所有用户
     * @return List<LdapPersonEntity>
     */
    List<LdapPersonEntity> findAll();

    /**
     * 更改用户密码
     * @param username 用户名
     * @param newPassword 新的明文密码
     */
    void changePassword(String username,String newPassword);

    /**
     * 修改用户信息（用户名为标识符）
     * @param user LdapPersonEntity
     */
    void saveUser(LdapPersonEntity user);

    /**
     * 新增用户
     * @param user LdapPersonEntity
     * @param password 未加密的密码
     */
    void createUser(LdapPersonEntity user, String password);

    /**
     * 删除用户
     * @param user LdapPersonEntity
     */
    void deleteUser(LdapPersonEntity user);

    /**
     * 添加管理员
     * @param username 用户名
     */
    void addAdmin(String username);

    /**
     * 删除管理员
     * @param username 用户名
     */
    void removeAdmin(String username);
}
