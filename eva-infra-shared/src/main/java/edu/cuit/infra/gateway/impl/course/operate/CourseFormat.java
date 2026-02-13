package edu.cuit.infra.gateway.impl.course.operate;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Component
public class CourseFormat {
    private final Object courOneEvaTemplateMapper;
    private final ObjectMapper objectMapper;

    public CourseFormat(@Qualifier("courOneEvaTemplateMapper") Object courOneEvaTemplateMapper,
                        ObjectMapper objectMapper) {
        this.courOneEvaTemplateMapper = courOneEvaTemplateMapper;
        this.objectMapper = objectMapper;
    }

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
        Method selectOneMethod = findMethod(courOneEvaTemplateMapper.getClass(), "selectOne", 1);
        if (selectOneMethod == null) {
            throw new IllegalStateException("courOneEvaTemplateMapper未提供selectOne方法");
        }
        Object courOneEvaTemplateDO = invoke(selectOneMethod, courOneEvaTemplateMapper,
                new QueryWrapper<>().eq("course_id", courseId).eq("semester_id", semId));
        if(courOneEvaTemplateDO==null)return null;

        Method getFormTemplateMethod = findMethod(courOneEvaTemplateDO.getClass(), "getFormTemplate", 0);
        if (getFormTemplateMethod == null) {
            throw new IllegalStateException("课程模板对象未提供getFormTemplate方法");
        }
        String format = toFormat((String) invoke(getFormTemplateMethod, courOneEvaTemplateDO));
        EvaTemplateCO evaTemplateCO=null;
        try {
            evaTemplateCO = objectMapper.readValue(format, EvaTemplateCO.class);
        } catch (JsonProcessingException e) {
            throw new ClassCastException("类型转换异常");
        }
        return evaTemplateCO;
    }

    private Method findMethod(Class<?> type, String methodName, int parameterCount) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        return null;
    }

    private Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        }
    }

    public static String getNatureName(Integer nature){
        return nature==1?"实验课":"理论课";
    }
}
