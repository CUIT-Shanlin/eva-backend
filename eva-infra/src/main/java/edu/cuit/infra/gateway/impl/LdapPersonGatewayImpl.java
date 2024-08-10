package edu.cuit.infra.gateway.impl;

import edu.cuit.domain.entity.user.LdapPersonEntity;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LdapPersonGatewayImpl implements LdapPersonGateway {

    private final LdapTemplate ldapTemplate;

    @Override
    public boolean authenticate(String username, String password) {
        return false;
    }

    @Override
    public Optional<LdapPersonEntity> findByUsername(String username) {
        return Optional.empty();
    }

    @Override
    public void saveUser(LdapPersonEntity user) {

    }
}
