package edu.cuit.client.dto.cmd.course;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.dto.query.course.CourseTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 一门课程的可修改信息(一门课程的可修改信息)
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class UpdateSingleCourseCmd extends ClientObject {
    /**
     * 课程详情id
     */
    private Integer id;

    /**
     * 授课教室
     */
    private String location;

    /**
     * =课程时间模型
     */
    private CourseTime time;
}
