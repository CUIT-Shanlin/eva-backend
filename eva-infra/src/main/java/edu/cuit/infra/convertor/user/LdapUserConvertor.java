package edu.cuit.infra.convertor.user;

import edu.cuit.domain.entity.user.LdapPersonEntity;
import edu.cuit.infra.convertor.EntityFactory;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.ldap.dataobject.LdapPersonDO;
import edu.cuit.infra.util.EvaLdapUtils;
import org.mapstruct.*;

/**
 * Ldap用户对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class)
public interface LdapUserConvertor {

    @Mappings({
            @Mapping(target = "name", expression = "java(ldapPersonDO.getSurname()+ldapPersonDO.getGivenName())"),
            @Mapping(target = "isAdmin", ignore = true),
            @Mapping(target = "ldapPersonGateway", ignore = true)
    })
    LdapPersonEntity ldapPersonDoToLdapPersonEntity(LdapPersonDO ldapPersonDO);

    @AfterMapping
    default void afterPersonDoToPersonEntity(LdapPersonDO personDO, @MappingTarget LdapPersonEntity personEntity) {
        EvaLdapUtils.getAdminGroupDo().ifPresentOrElse(groupDo -> personEntity.setIsAdmin(groupDo.getMembers().contains(personDO.getUsername())),() -> personEntity.setIsAdmin(false));
    }

    @Mappings({
            @Mapping(target = "commonName", source = "userDO.name"),
            @Mapping(target = "gidNumber", ignore = true),
            @Mapping(target = "homeDirectory", ignore = true),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "uidNumber", ignore = true),
            @Mapping(target = "userPassword", ignore = true),
            @Mapping(target = "givenName", expression = "java(userDO.getName().substring(1))"),
            @Mapping(target = "school", source = "userDO.department"),
            @Mapping(target = "surname", expression = "java(userDO.getName().substring(0,1))"),
            @Mapping(target = "title", source = "userDO.profTitle")
    })
    LdapPersonDO userDOToLdapPersonDO(SysUserDO userDO);

    @Mappings({
            @Mapping(target = "commonName", ignore = true),
            @Mapping(target = "gidNumber", ignore = true),
            @Mapping(target = "homeDirectory", ignore = true),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "uidNumber", ignore = true),
            @Mapping(target = "userPassword", ignore = true)
    })
    LdapPersonDO ldapPersonEntityToLdapPersonDO(LdapPersonEntity ldapPersonEntity);
}
