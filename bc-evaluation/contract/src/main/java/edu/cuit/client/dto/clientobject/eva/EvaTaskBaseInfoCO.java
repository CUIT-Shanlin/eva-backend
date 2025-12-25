package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

/**
 * 评教任务基础信息，用于显示一个任务基础信息，后端=》前端
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class EvaTaskBaseInfoCO extends ClientObject {

    /**
     * id
     */
    private Long id;

    /**
     * 任务状态（0：待执行，1：已执行）
     */
    private Long status;

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
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

