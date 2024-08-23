package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;

import edu.cuit.client.dto.data.course.CourseTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

/**
 * 一条评教记录-完整
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class EvaRecordCO extends ClientObject {

    /**
     * id
     */
    private Long id;

    /**
     * 表单模板id
     */
    private Long templateId;

    /**
     * 教学老师的姓名
     */
    private String teacherName;

    /**
     * 评教老师的姓名
     */
    private String evaTeacherName;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 详细的文字评价
     */
    private String textValue;

    /**
     * 平均分
     */
    private Double averScore;

    /**
     * 表单评教指标对应的分值，JSON表示的字符串形式
     */
    private String formPropsValues;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 上课的时间，xx周 星期x  x节课到x节课
     */
    private CourseTime courseTime;
}
