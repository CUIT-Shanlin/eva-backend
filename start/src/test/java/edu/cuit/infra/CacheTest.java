package edu.cuit.infra;

import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@SpringBootTest
public class CacheTest {

    @Autowired
    public UserQueryGateway userQueryGateway;

    @Autowired
    public SysUserMapper userMapper;

    @Test
    public void testCache() {
        Instant start = Instant.now();
        UserEntity u1 = userQueryGateway.findById(1).get();
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        System.out.println(u1.getName() + " ," + duration.toNanos());
        SysUserDO sysUserDO = new SysUserDO()
                .setId(1)
                .setName("你好");
        userMapper.updateById(sysUserDO);

        Instant start2 = Instant.now();
        UserEntity u2 = userQueryGateway.findById(1).get();
        Instant end2 = Instant.now();
        Duration duration2 = Duration.between(start2, end2);
        System.out.println(u2.getName() + " ," + duration2.toNanos());

    }

}
