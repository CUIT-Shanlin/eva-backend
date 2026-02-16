package edu.cuit.infra.dal.database.dataobject.course;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 课程表（一门课程）
 * @TableName course
 */
@TableName(value ="course")
@Data
public class CourseDO implements Serializable {
    /**
     * 课程id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 科目的id
     */
    @TableField(value = "subject_id")
    private Integer subjectId;

    /**
     * 教学老师id
     */
    @TableField(value = "teacher_id")
    private Integer teacherId;


    /**
     * 学期id
     */
    @TableField(value = "semester_id")
    private Integer semesterId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    /**
     * 实现逻辑删除（0:不可用 1:可用）
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /**
     * 模板id，仅在有快照数据前生效
     */
    @TableField(value = "templateId")
    private Integer templateId;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}