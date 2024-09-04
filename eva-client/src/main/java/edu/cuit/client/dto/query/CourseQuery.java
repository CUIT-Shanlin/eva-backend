package edu.cuit.client.dto.query;

import com.alibaba.cola.dto.Query;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课程查询
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CourseQuery extends Query {

    /**
     * 周
     */
    private Integer week;

    /**
     * 星期几
     */
    private Integer day;

    /**
     * 第几节
     */
    private Integer num;

}
