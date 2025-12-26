package edu.cuit.client.dto.clientobject;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/**
 * 学期模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class SemesterCO extends ClientObject {

    /**
     * id
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

    /**
     * 这学期开学第一天的日期
     */
    private LocalDate startDate;

}
