package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.dto.data.course.CourseTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 评教任务-详细信息，获取自己的评教任务的详情信息，后端=》前端
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class EvaTaskDetailInfoCO extends ClientObject {

    /**
     * 任务id
     */
    private Long id;
    /**
     * 任务状态（0：待执行，1：已执行，2：已撤回）
     */
    private Integer status;
    /**
     * 发起评教任务的老师姓名
     */
    private String evaTeacherName;
    /**
     * 教学老师姓名
     */
    private String teacherName;
    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 创始时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 教室
     */
    private String location;

    /**
     * 课程时间
     */
    private CourseTime courseTime;
}
