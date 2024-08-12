package edu.cuit.infra.dal.ldap.dataobject;

import edu.cuit.infra.enums.LdapConstant;
import lombok.Data;
import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import javax.naming.Name;
import java.util.ArrayList;
import java.util.List;

/**
 * Ldap组信息
 */
@Entry(
        base = "ou=member,ou=group",//FIXME 动态设置
        objectClasses = {"posixGroup"}
)
@Data
public class LdapGroupDO {

    @Id
    private Name id;

    /**
     * 组名
     */
    @Attribute(name = "cn")
    private String commonName;

    /**
     * 组员（保存用户的用户名uid）
     */
    @Attribute(name = "memberUid")
    private List<String> members = new ArrayList<>();
}
