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
 * 课程详情表
 * @TableName cour_inf
 */
@TableName(value ="cour_inf")
@Data
public class CourInfDO implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 课程id
     */
    @TableField(value = "course_id")
    private Integer course_id;

    /**
     * 上课周时间
     */
    @TableField(value = "week")
    private Integer week;

    /**
     * 开始时间（第几节开始）
     */
    @TableField(value = "start_time")
    private Integer start_time;

    /**
     * 结束时间（第几节结束）
     */
    @TableField(value = "end_time")
    private Integer end_time;

    /**
     * 学期id
     */
    @TableField(value = "semester_id")
    private Integer semester_id;

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
     * 逻辑删除
     */
    @TableField(value = "is_deleted")
    private Integer is_deleted;

    /**
     * 地点信息
     */
    @TableField(value = "location")
    private String location;

    /**
     * 星期几
     */
    @TableField(value = "day")
    private Integer day;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}