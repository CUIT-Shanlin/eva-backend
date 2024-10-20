package edu.cuit.infra;

import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import edu.cuit.infra.dal.database.mapper.user.SysRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UpdateTest {

    @Autowired
    public SysRoleMapper roleMapper;

    @Test
    public void testUpdate() {
        SysRoleDO sysRoleDO = new SysRoleDO();
        sysRoleDO.setId(1)
                .setStatus(1);
        roleMapper.updateById(sysRoleDO);
        System.out.println(sysRoleDO.toString());
    }

}
