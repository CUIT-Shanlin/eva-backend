package edu.cuit.infra.dal.po.course;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 课程表
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
    private Integer subject_id;

    /**
     * 评教模板的id
     */
    @TableField(value = "template_id")
    private Integer template_id;

    /**
     * 教学老师id
     */
    @TableField(value = "teachar_id")
    private Integer teachar_id;

    /**
     * JSON形式存教室数组
     */
    @TableField(value = "classroom")
    private String classroom;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime create_time;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private LocalDateTime update_time;

    /**
     * 实现逻辑删除（0:不可用 1:可用）
     */
    @TableField(value = "is_deleted")
    private Integer is_deleted;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}