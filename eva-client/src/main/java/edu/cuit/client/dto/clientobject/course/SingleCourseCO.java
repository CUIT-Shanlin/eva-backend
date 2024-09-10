package edu.cuit.client.dto.clientobject.course;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 一节课的模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class SingleCourseCO extends ClientObject {
    /**
     * 课程详情id
     */
    private Integer id;

    /**
     * 课程名称
     */
    private String name;

    /**
     * 教学老师姓名
     */
    private String teacherName;

    /**
     * 评教老师数量（已经选了这节课的老师的数目）
     */
    private Integer evaNum;

    /**
     * 开始时间（节数）
     */
    private Integer startTime;

    /**
     * 结束时间（节数）
     */
    private Integer endTime;
}
