package edu.cuit.client.dto.clientobject.course;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 自己教学的一门课程的一个课程时段模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class SelfTeachCourseTimeCO extends ClientObject {

    /**
     * 星期的数组
     */
    private List<Integer> weeks;

    /**
     * 星期几
     */
    private Integer day;

    /**
     * 开始时间
     */
    private Integer startTime;

    /**
     * 结束时间
     */
    private Integer endTime;

    /**
     * 教室
     */
    private String classroom;


}
