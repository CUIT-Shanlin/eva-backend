package edu.cuit.client.dto.clientobject.course;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 课程时间模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class SemesterCO extends ClientObject {

    /**
     * 课程id
     */
    private Integer id;

    /**
     * 上下学期，0为上，1为下
     */
    private Integer period;

    /**
     * 开始年份，如2023
     */
    private String startYear;

    /**
     * 结束年份
     */
    private String endYear;
}
