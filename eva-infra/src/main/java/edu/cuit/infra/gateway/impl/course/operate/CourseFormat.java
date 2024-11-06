package edu.cuit.infra.gateway.impl.course.operate;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CourseFormat {
    private final CourOneEvaTemplateMapper courOneEvaTemplateMapper;
    private final ObjectMapper objectMapper;
    public static String toFormat(String str){
        StringBuffer string = new StringBuffer();
        int flag=0;
        for (char c : str.toCharArray()) {
            if(c=='[') flag=1;
            if(c==']') flag=0;
            if(flag==1&&c=='"') string.append("\\");
            string.append(c);
        }
        return string.toString();
    }
    public EvaTemplateCO selectCourOneEvaTemplateDO(Integer semId, Integer courseId){
        CourOneEvaTemplateDO courOneEvaTemplateDO = courOneEvaTemplateMapper.selectOne(new QueryWrapper<CourOneEvaTemplateDO>().eq("course_id", courseId).eq("semester_id", semId));
        if(courOneEvaTemplateDO==null)return null;
        String format = toFormat(courOneEvaTemplateDO.getFormTemplate());
        EvaTemplateCO evaTemplateCO=null;
        try {
            evaTemplateCO = objectMapper.readValue(format, EvaTemplateCO.class);
        } catch (JsonProcessingException e) {
            throw new ClassCastException("类型转换异常");
        }
        return evaTemplateCO;
    }
    public static String getNatureName(Integer nature){
        return nature==1?"实验课":"理论课";
    }
}
