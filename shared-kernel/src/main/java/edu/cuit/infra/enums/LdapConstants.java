package edu.cuit.infra.enums;

import edu.cuit.infra.property.EvaLdapProperties;

import java.lang.reflect.Field;

/**
 * Ldap相关常量
 */
public class LdapConstants {

    // 用户存储域
    public static final String USER_BASE_DN;

    // 用户组存储域
    public static final String GROUP_BASE_DN;

    static {
        try {
            Class<?> evaLdapUtilsClass = Class.forName("edu.cuit.infra.util.EvaLdapUtils");
            Field evaLdapPropertiesField = evaLdapUtilsClass.getField("evaLdapProperties");
            EvaLdapProperties evaLdapProperties = (EvaLdapProperties) evaLdapPropertiesField.get(null);

            USER_BASE_DN = evaLdapProperties.getUserBaseDn();
            GROUP_BASE_DN = evaLdapProperties.getGroupBaseDn();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
