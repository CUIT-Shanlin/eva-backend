package edu.cuit.infra;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import edu.cuit.client.bo.EvaProp;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import edu.cuit.infra.dal.database.mapper.user.SysRoleMapper;
import edu.cuit.infra.gateway.impl.course.operate.CourseFormat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

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
        String str="[\"教学目标和教学计划的合理性\",\"技术内容的准确性和前沿性\",\"教学过程的设计和组织能力\",\"团队合作与项目管理的引导\",\"课程资源的丰富性和可用性\",\"对学生未来职业发展的指导\"]";
        JSONArray jsonArray = JSONUtil.parseArray(str);
        List<String> list = jsonArray.toList(String.class);
        System.out.println(list);


    }

}
