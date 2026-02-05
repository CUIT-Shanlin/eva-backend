package edu.cuit.domain.entity.course;

import com.alibaba.cola.domain.Entity;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

/**
 * 一学期domain entity
 */
@Entity
@Data
@RequiredArgsConstructor
public class SemesterEntity {

    /**
     * id
     */
    private Integer id;

    /**
     * 开始年份，如2023
     */
    private String startYear;

    /**
     * 上下学期，0为上，1为下
     */
    private Integer period;

    /**
     * 结束年份
     */
    private String endYear;

    /**
     * 学期开始日期（需是星期一）
     */
    private LocalDate startDate;

}
