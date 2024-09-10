package edu.cuit.client.dto.clientobject.course;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.dto.data.course.CourseTime;
import edu.cuit.client.dto.data.course.CourseType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;
/**
 * 针对于“获取某个指定时间段的课程”接口的衍生类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class ModifySingleCourseDetailCO extends ClientObject {
    /**
     * 教室
     */
    private String location;

    /**
     * 课程性质(0:理论课,1:实验课,3:其他)
     */
    private Integer nature;

    /**
     * 类型数组
     */
    private List<CourseType> typeList;

    /**
     * 评教老师数
     */
    private Integer evaTeacherNum;

    /**
     * 一节课的模型
     */
    private SingleCourseCO course;

    /**
     *课程事件模型
     */
    private CourseTime time;
}
