package edu.cuit.infra.convertor.user;

import edu.cuit.domain.entity.user.LdapPersonEntity;
import edu.cuit.infra.dal.ldap.dataobject.LdapPersonDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface UserConvertor {

    @Mappings(
            @Mapping(target = "name",expression = "java(ldapPersonDO.getSurname()+ldapPersonDO.getGivenName())")
    )
    LdapPersonEntity ldapPersonDoToLdapPersonEntity(LdapPersonDO ldapPersonDO);

    LdapPersonDO ldapPersonEntityToLdapPersonDO(LdapPersonEntity ldapPersonEntity);
}
