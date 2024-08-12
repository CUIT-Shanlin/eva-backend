package edu.cuit.infra.user;

import cn.hutool.extra.spring.SpringUtil;
import edu.cuit.Application;
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
        System.out.println(ldapPersonGateway.authenticate("1", "456123"));
        Optional<LdapPersonEntity> byUsername = ldapPersonGateway.findByUsername("1");
        LdapPersonEntity ldapPersonEntity = byUsername.get();
        System.out.println(ldapPersonEntity.getName());
        System.out.println(ldapPersonEntity.getEmail());
        System.out.println(ldapPersonEntity.getIsAdmin());
    }

    @Test
    public void testSaveUser() {
        LdapPersonEntity person = SpringUtil.getBean(LdapPersonEntity.class);
        person.setEmail("woshisb@sb.sb");
        person.setName("杜锟浩");
        person.setSchool("大SB学院");
        person.setPhone("54313");
        person.setSurname("杜");
        person.setGivenName("锟浩");
        person.setUsername("2");
        ldapPersonGateway.createUser(person,"456123");
    }

    @Test
    public void testAddRemoveAdmin() {
        ldapPersonGateway.findByUsername("2").ifPresent(ldapPersonEntity -> System.out.println(ldapPersonEntity.getIsAdmin()));
        ldapPersonGateway.removeAdmin("2");
        ldapPersonGateway.findByUsername("2").ifPresent(ldapPersonEntity -> System.out.println(ldapPersonEntity.getIsAdmin()));
    }

}
