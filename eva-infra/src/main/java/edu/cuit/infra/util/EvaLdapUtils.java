package edu.cuit.infra.util;

import cn.hutool.extra.spring.SpringUtil;
import edu.cuit.infra.dal.ldap.dataobject.LdapGroupDO;
import edu.cuit.infra.dal.ldap.repo.LdapGroupRepo;
import edu.cuit.infra.enums.LdapConstant;
import edu.cuit.infra.property.EvaLdapProperties;
import org.springframework.boot.autoconfigure.ldap.LdapProperties;
import org.springframework.ldap.support.LdapUtils;

import javax.naming.ldap.LdapName;
import java.util.Optional;

/**
 * Ldap相关工具类
 */
public class EvaLdapUtils {

    public static final EvaLdapProperties evaLdapProperties;
    private static final LdapGroupRepo ldapGroupRepo;

    static {
        evaLdapProperties = SpringUtil.getBean(EvaLdapProperties.class);
        ldapGroupRepo = SpringUtil.getBean(LdapGroupRepo.class);
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

    /**
     * 获取管理员组do
     */
    public static Optional<LdapGroupDO> getAdminGroupDo() {
        return ldapGroupRepo.findByCommonName(EvaLdapUtils.evaLdapProperties.getAdminGroupCn());
    }

}
