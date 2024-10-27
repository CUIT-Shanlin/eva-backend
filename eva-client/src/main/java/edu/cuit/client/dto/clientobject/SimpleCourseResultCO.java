package edu.cuit.client.dto.clientobject;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;


/**
 * 极简响应模型-一门课程
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class SimpleCourseResultCO extends ClientObject {

    /**
     * id
     */
    private Integer id;

    /**
     * 名称
     */
    private String name;

    /**
     * 教学老师姓名
     */
    private String teacherName;

    /**
     * 课程性质(0:理论课,1:实验课,3:其他)
     */
    private Integer nature;
}
