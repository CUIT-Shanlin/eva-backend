package edu.cuit.client.dto.query.condition;

import edu.cuit.client.dto.query.course.CourseTime;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 评教记录条件查询模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EvaConditionalQuery extends ConditionalQuery {

    /**
     * 课程id数组，课程id数组
     */
    private List<Long> courseIds;

    /**
     * 上课时间数组，上课时间段数组
     */
    @Valid
    private List<CourseTime> courseTimes;

    /**
     * 学院名称，学院名称
     */
    private String departmentName;

    /**
     * 结束的评教时间，筛选评教时间的结束时间
     */
    private String endEvaluateTime;

    /**
     * 评教老师的id数组，评教老师的id数组
     */
    private List<Long> evaTeacherIds;

    /**
     * 最高分，最高分
     */
    private double maxScore;

    /**
     * 最低分，最低分
     */
    private double minScore;

    /**
     * 开始的评教时间，筛选评教时间的开始时间
     */
    private String startEvaluateTime;

    /**
     * 教学教师的id数组，教学教师的id数组
     */
    private List<Long> teacherIds;

}
