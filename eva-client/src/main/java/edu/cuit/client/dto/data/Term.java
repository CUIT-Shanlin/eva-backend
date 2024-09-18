package edu.cuit.client.dto.data;

import com.alibaba.cola.dto.DTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Range;

/**
 * 课程的一段时间的模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class Term extends DTO {
    /**
     * 学期id
     */
    private Integer id;

    /**
     * 上下学期，0为上，1为下
     */
    @Range(min = 0,max = 1,message = "学期必须在0和1之间")
    private Integer period;

    /**
     * 开始年份，如2023
     */
    private String startYear;

    /**
     * 结束年份
     */
    private String endYear;

    /**
     * 这学期开学第一天的日期
     */
    private String startDate;

}
