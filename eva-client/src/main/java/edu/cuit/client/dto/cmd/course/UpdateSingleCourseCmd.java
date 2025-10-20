package edu.cuit.client.dto.cmd.course;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.dto.data.course.CourseTime;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 一节课的可修改信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class UpdateSingleCourseCmd extends ClientObject {
    /**
     * 课程详情id
     */
    @NotNull(message = "课程详情id不能为空")
    private Integer id;

    /**
     * 授课教室
     */
    @NotNull(message = "授课教室不能为空")
    private String location;

    /**
     * 课程时间模型
     */
    @NotNull(message = "课程时间不能为空")
    private CourseTime time;
}
