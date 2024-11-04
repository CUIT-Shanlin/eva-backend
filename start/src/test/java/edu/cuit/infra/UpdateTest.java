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
                .setStatus(0);
        roleMapper.updateById(sysRoleDO);
        System.out.println(sysRoleDO.toString());
    }
    @Test
    public void testUpdate2() {
//        String str="{\"name\":\"实验课默认模板\",\"description\":\"实验课通用的默认评教模板\",\"props\":\"[\\\"教学目标和教学计划的合理性\\\",\\\"团队合作与项目管理的引导\\\",\\\"课程资源的丰富性和可用性\\\",\\\"教学目标和教学计划的合理性(1)\\"]}"
    }

}
