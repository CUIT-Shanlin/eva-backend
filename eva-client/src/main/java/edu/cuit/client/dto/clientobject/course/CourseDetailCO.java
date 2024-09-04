package edu.cuit.client.dto.clientobject.course;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.dto.data.course.CourseTime;
import edu.cuit.client.dto.data.course.CourseType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 一门课程的详情
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class CourseDetailCO extends ClientObject {

    /**
     * 课程类型数组
     */
    private List<CourseType> typeList;

    /**
     * 上课时间数组
     */
    private List<CourseTime> dateList;

    /**
     * 一门课程的模型
     */
    private CourseModuleCO courseBaseMsg;

}
