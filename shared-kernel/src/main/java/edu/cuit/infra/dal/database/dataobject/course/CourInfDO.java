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
 * 课程详情表（一节课程）
 * @TableName cour_inf
 */
@TableName(value ="cour_inf")
@Data
public class CourInfDO implements Serializable,Cloneable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 课程id
     */
    @TableField(value = "course_id")
    private Integer courseId;

    /**
     * 上课周时间
     */
    @TableField(value = "week")
    private Integer week;

    /**
     * 开始时间（第几节开始）
     */
    @TableField(value = "start_time")
    private Integer startTime;

    /**
     * 结束时间（第几节结束）
     */
    @TableField(value = "end_time")
    private Integer endTime;

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
     * 逻辑删除
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

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

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    protected Object clone()  {
        CourInfDO courInfDO = new CourInfDO();
        courInfDO.setCourseId(courseId);
        courInfDO.setWeek(week);
        courInfDO.setStartTime(startTime);
        courInfDO.setEndTime(endTime);
        courInfDO.setLocation(location);
        courInfDO.setDay(day);
        return courInfDO;
    }
}