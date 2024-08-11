package edu.cuit;

import edu.cuit.domain.entity.user.LdapPersonEntity;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

@SpringBootTest(classes = Application.class)
public class TestLdap {

    @Autowired
    LdapPersonGateway ldapPersonGateway;

    @Test
    public void testAuthenticate() {
        System.out.println(ldapPersonGateway.authenticate("1", "123456"));
        Optional<LdapPersonEntity> byUsername = ldapPersonGateway.findByUsername("1");
        System.out.println(byUsername.isEmpty());
        System.out.println(byUsername.get().getName());
        System.out.println(byUsername.get().getEmail());
    }

}
