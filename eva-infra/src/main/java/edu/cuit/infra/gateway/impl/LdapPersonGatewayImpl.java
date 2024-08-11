package edu.cuit.infra.gateway.impl;

import cn.hutool.core.util.IdUtil;
import edu.cuit.domain.entity.user.LdapPersonEntity;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import edu.cuit.infra.convertor.user.UserConvertor;
import edu.cuit.infra.dal.ldap.dataobject.LdapPersonDO;
import edu.cuit.infra.dal.ldap.repo.LdapPersonRepo;
import edu.cuit.infra.enums.LdapConstant;
import edu.cuit.infra.util.EvaLdapUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LdapPersonGatewayImpl implements LdapPersonGateway {

    private final LdapTemplate ldapTemplate;
    private final LdapPersonRepo ldapPersonRepo;
    private final UserConvertor userConvertor;

    @Override
    public boolean authenticate(String username, String password) {
        EqualsFilter equalsFilter = new EqualsFilter("uid",username);
        return ldapTemplate.authenticate(LdapConstant.USER_BASE_DN,equalsFilter.encode(),password);
    }

    @Override
    public Optional<LdapPersonEntity> findByUsername(String username) {
        Optional<LdapPersonDO> personDO = ldapPersonRepo.findByUsername(username);
        return personDO.map(userConvertor::ldapPersonDoToLdapPersonEntity);
    }

    @Override
    public void saveUser(LdapPersonEntity user,String password) {
        LdapPersonDO personDO = userConvertor.ldapPersonEntityToLdapPersonDO(user);
        personDO.setUserPassword(password);
        personDO.setId(EvaLdapUtils.getUserLdapNameId(personDO.getUsername()));
        personDO.setCommonName(personDO.getSurname() + personDO.getGivenName());
        personDO.setUidNumber(IdUtil.getSnowflakeNextIdStr());
        personDO.setHomeDirectory("/home/" + user.getUsername());
        ldapTemplate.create(personDO);
    }
}
