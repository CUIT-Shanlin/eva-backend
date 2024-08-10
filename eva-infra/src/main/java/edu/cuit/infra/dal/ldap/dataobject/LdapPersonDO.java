package edu.cuit.infra.dal.ldap.dataobject;

import lombok.Data;
import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import javax.naming.Name;

/**
 * Ldap用户信息
 */
@Entry(
        base = "ou=member,ou=user",
        objectClasses = {"inetOrgPerson","posixAccount","organizationalPerson","person"}
)
@Data
public class LdapPersonDO {

    @Id
    private Name id;

    /**
     * 登录用用户名
     */
    @Attribute(name = "uid")
    private String username;

    /**
     * 昵称
     */
    @Attribute(name = "cn")
    private String name;

    /**
     * 邮箱
     */
    @Attribute(name = "mail")
    private String email;

    /**
     * 手机号
     */
    @Attribute(name = "mobile")
    private String phone;

    /**
     * 学院
     */
    @Attribute(name = "o")
    private String school;


}
