package edu.cuit.infra;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import edu.cuit.client.bo.EvaProp;
import org.junit.jupiter.api.Test;

import java.util.List;

public class UpdateTest {
    @Test
    public void testJsonArrayToList() {
        String str="[\"教学目标和教学计划的合理性\",\"技术内容的准确性和前沿性\",\"教学过程的设计和组织能力\",\"团队合作与项目管理的引导\",\"课程资源的丰富性和可用性\",\"对学生未来职业发展的指导\"]";
        JSONArray jsonArray = JSONUtil.parseArray(str);
        List<String> list = jsonArray.toList(String.class);
        org.junit.jupiter.api.Assertions.assertEquals(6, list.size());
        org.junit.jupiter.api.Assertions.assertEquals("教学目标和教学计划的合理性", list.get(0));

    }

}
