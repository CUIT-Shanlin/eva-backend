package edu.cuit.infra.convertor.user;

import edu.cuit.domain.entity.user.LdapPersonEntity;
import edu.cuit.infra.dal.ldap.dataobject.LdapPersonDO;
import edu.cuit.infra.util.EvaLdapUtils;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserConvertor {

    @Mappings(
            @Mapping(target = "name",expression = "java(ldapPersonDO.getSurname()+ldapPersonDO.getGivenName())")
    )
    LdapPersonEntity ldapPersonDoToLdapPersonEntity(LdapPersonDO ldapPersonDO);

    @AfterMapping
    default void afterPersonDoToPersonEntity(LdapPersonDO personDO, @MappingTarget LdapPersonEntity personEntity) {
        EvaLdapUtils.getAdminGroupDo().ifPresentOrElse(groupDo -> personEntity.setIsAdmin(groupDo.getMembers().contains(personDO.getUsername())),() -> personEntity.setIsAdmin(false));
    }

    LdapPersonDO ldapPersonEntityToLdapPersonDO(LdapPersonEntity ldapPersonEntity);
}
