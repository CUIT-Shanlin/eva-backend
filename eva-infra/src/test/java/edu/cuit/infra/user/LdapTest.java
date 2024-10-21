package edu.cuit.infra.user;

import edu.cuit.domain.entity.user.LdapPersonEntity;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

@SpringBootTest
public class LdapTest {

    @Autowired
    private LdapPersonGateway ldapPersonGateway;

    @Test
    public void testQuery() {
        Optional<LdapPersonEntity> byUsername = ldapPersonGateway.findByUsername("111");
        byUsername.ifPresent(personEntity -> {
            System.out.println(personEntity.getName());
            System.out.println(personEntity.getEmail());
        });
//        ldapPersonGateway.changePassword("111","456789");
        System.out.println(ldapPersonGateway.authenticate("111", "456789"));
    }

}
