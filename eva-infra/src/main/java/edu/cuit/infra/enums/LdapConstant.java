package edu.cuit.infra.enums;

import edu.cuit.infra.util.EvaLdapUtils;

/**
 * Ldap相关常量
 */
public class LdapConstant {

    // 用户存储域
    public static final String USER_BASE_DN;

    // 用户组存储域
    public static final String GROUP_BASE_DN;

    static {
        USER_BASE_DN = EvaLdapUtils.evaLdapProperties.getUserBaseDn();
        GROUP_BASE_DN = EvaLdapUtils.evaLdapProperties.getGroupBaseDn();
    }
}
