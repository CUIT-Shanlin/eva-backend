package edu.cuit.client.dto.query.condition;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课程条件查询模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CourseConditionalQuery extends GenericConditionalQuery {


    /**
     * 学院名称
     */
    private String departmentName;

    /**
     *评教模板的id
     */
    private Integer templateId;

}
