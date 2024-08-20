package edu.cuit.client.dto.clientobject.course;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 *
 *
 * 一门课程的详情
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class CourseDetailsCO extends CourseModuleCO {
    /**
     * 课程类型数组
     */
    private List<Object> typeList;
    /**
     * 上课时间数组
     */
    private List<Object> dateList;

}
