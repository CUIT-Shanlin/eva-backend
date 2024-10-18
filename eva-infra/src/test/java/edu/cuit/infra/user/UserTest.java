package edu.cuit.infra.user;

import edu.cuit.domain.gateway.user.UserUpdateGateway;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserTest {

    @Autowired
    private SysUserMapper userMapper;

    @Test
    public void testInsertUser() {
        SysUserDO sysUserDO = new SysUserDO();
        sysUserDO.setUsername("123");
        System.out.println(userMapper.insert(sysUserDO));
        System.out.println(sysUserDO.getId());
    }

}
