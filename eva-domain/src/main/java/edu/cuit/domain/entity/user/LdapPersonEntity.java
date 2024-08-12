package edu.cuit.domain.entity.user;

import com.alibaba.cola.domain.Entity;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * ldap用户domain entity
 */
@Entity
@Data
@RequiredArgsConstructor
public class LdapPersonEntity {

    /**
     * 登录用用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String name;

    /**
     * 姓
     */
    private String surname;

    /**
     * 名
     */
    private String givenName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 学院(系)
     */
    private String school;

    /**
     * 职称
     */
    private String title;

    /**
     * 是否为管理员
     */
    private Boolean isAdmin;

    private final Optional<LdapPersonGateway> ldapPersonGateway;

}
