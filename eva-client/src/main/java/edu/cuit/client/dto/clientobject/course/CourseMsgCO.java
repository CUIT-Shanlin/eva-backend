package edu.cuit.client.dto.clientobject.course;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 一门科目(一门科目)
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class CourseMsgCO extends ClientObject {
    /**
     * 课程id
     */
    private Integer id;

    /**
     * 课程名称
     */
    private String name;

    /**
     * 课程性质(0:理论课,1:实验课,3:其他)
     */
    private Integer nature;

    /**
     * 课程创建时间
     */
    private String createTime;

    /**
     * 课程更新时间
     */
    private String updateTime;
}
