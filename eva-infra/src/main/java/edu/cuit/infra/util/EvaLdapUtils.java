package edu.cuit.infra.util;

import cn.hutool.extra.spring.SpringUtil;
import edu.cuit.infra.enums.LdapConstant;
import edu.cuit.infra.property.EvaLdapProperties;
import org.springframework.boot.autoconfigure.ldap.LdapProperties;
import org.springframework.ldap.support.LdapUtils;

import javax.naming.ldap.LdapName;

/**
 * Ldap相关工具类
 */
public class EvaLdapUtils {

    private static final LdapProperties ldapProperties;
    public static final EvaLdapProperties evaLdapProperties;

    static {
        ldapProperties = SpringUtil.getBean(LdapProperties.class);
        evaLdapProperties = SpringUtil.getBean(EvaLdapProperties.class);
    }

    /**
     * 获取用户Name(ldap id)
     * @param uid 用户名
     * @return LdapName
     */
    public static LdapName getUserLdapNameId(String uid) {
        return LdapUtils.newLdapName("uid=" + uid + "," + LdapConstant.USER_BASE_DN);
    }

    /**
     * 获取管理员组DN
     */
    public static String getAdminGroupDn() {
        return "cn=" + evaLdapProperties.getAdminGroupCn() + "," + LdapConstant.GROUP_BASE_DN;
    }

}
