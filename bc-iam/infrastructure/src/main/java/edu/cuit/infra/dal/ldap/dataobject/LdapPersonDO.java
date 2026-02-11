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
     * 姓
     */
    @Attribute(name = "sn")
    private String surname;

    /**
     * 名
     */
    @Attribute(name = "givenName")
    private String givenName;

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
     * 学院(系)
     */
    @Attribute(name = "o")
    private String school;

    /**
     * 职称
     */
    @Attribute(name = "title")
    private String title;

    /**
     * posixAccount要求该属性
     */
    @Attribute(name="gidNumber")
    private String gidNumber = "10000";

    /**
     * posixAccount要求该属性
     */
    @Attribute(name="uidNumber")
    private String uidNumber;

    /**
     * posixAccount要求该属性
     */
    @Attribute(name="homeDirectory")
    private String homeDirectory;

    /**
     * 昵称
     */
    @Attribute(name = "cn")
    private String commonName;


}
