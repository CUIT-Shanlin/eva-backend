package edu.cuit.client.dto.clientobject.course;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 一门课程的信息模型(一门课程的模型)
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class CourseModuleCO extends ClientObject {

    /**
     * 课程id
     */
    private Integer id;

    /**
     * 教室数组
     */
    private List<String> classroomList;

    /**
     * 科目信息
     */
    private CourseMsgCO subjectMsg;

    /**
     *评教模板信息
     */
    private EvaTemplateCO template;

    /**
     *教学老师信息信息
     */
    private TeacherMsg teacherMsg;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
