package edu.cuit.client.dto.cmd.course;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 *针对于“批量新建多节课(新课程)”
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class AddCoursesAndCourInfoCmd extends ClientObject {
    /**
     * 一门课程的可修改信息
     */
    @NotNull(message = "新课程信息不能为空")
    private UpdateCourseCmd courseInfo;

    /**
     * 自己教学的一门课程的一个课程时段模型
     */
    private List<SelfTeachCourseTimeCO> dateArr;

}
