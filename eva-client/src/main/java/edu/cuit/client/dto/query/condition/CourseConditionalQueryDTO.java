package edu.cuit.client.dto.query.condition;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课程条件查询模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CourseConditionalQueryDTO extends ConditionalQueryDTO{

    /**
     * 老师id
     */
    private Integer teacherId;

    /**
     * 学院名称
     */
    private String departmentName;

}
