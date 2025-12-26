package edu.cuit.client.dto.cmd.course;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeInfoCO;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 针对于“修改自己的一门课程信息及其课程时段”
 * 接口的接受类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class UpdateCourseInfoAndTimeCmd extends ClientObject {

    /**
     * 这门课程的新信息
     */
    @NotNull(message = "课程信息不能为空")
    private SelfTeachCourseCO courseInfo;

    /**
     * 新的课程时段
     */
    @NotNull(message = "课程时段信息不能为空")
    private List<SelfTeachCourseTimeInfoCO> dateArr;
}
