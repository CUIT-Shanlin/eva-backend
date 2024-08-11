package edu.cuit.infra.util;

import cn.hutool.extra.spring.SpringUtil;
import edu.cuit.infra.enums.LdapConstant;
import org.springframework.boot.autoconfigure.ldap.LdapProperties;
import org.springframework.ldap.support.LdapUtils;

import javax.naming.ldap.LdapName;

/**
 * Ldap相关工具类
 */
public class LdapUtil {

    private static final LdapProperties ldapProperties;

    static {
        ldapProperties = SpringUtil.getBean(LdapProperties.class);
    }

    public static LdapName getUserLdapNameId(String uid) {
        return LdapUtils.newLdapName("uid=" + uid + "," + LdapConstant.USER_BASE_DN);
    }

}
