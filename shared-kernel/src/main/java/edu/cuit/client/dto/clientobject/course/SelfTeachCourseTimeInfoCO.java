package edu.cuit.client.dto.clientobject.course;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 针对于“获取自己教学的一门课程的课程时段”衍生类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class SelfTeachCourseTimeInfoCO extends ClientObject {
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
    private List<String> classroom;
}
