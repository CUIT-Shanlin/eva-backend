package edu.cuit.infra.enums;

/**
 * Ldap相关常量
 */
public interface LdapConstant {

    // 用户存储域
    String USER_BASE_DN = "ou=member,ou=user";

    // 用户组存储域
    String GROUP_BASE_DN = "ou=member,ou=group";
}
