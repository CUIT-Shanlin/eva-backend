package edu.cuit.infra.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eva.ldap")
@Data
public class EvaLdapProperties {

    private String userBaseDn = "ou=member,ou=user";

    private String groupBaseDn = "ou=member,ou=group";

    private String adminGroupCn = "admin";

}
