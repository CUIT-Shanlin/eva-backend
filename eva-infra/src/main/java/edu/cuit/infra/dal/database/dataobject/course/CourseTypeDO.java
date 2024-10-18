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
 * 课程类型
 * @TableName course_type
 */
@TableName(value ="course_type")
@Data
public class CourseTypeDO implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 类型名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 是否为默认课程类型
     */
    @TableField(value = "is_default")
    private Integer isDefault;

    /**
     * 描述
     */
    @TableField(value = "description")
    private String description;

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
     * 删除标记（0:不可用 1:可用）
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /**
     * 判断该数据是否是默认数据，0: 理论课相关默认；1: 实验课相关默认；-1：非默认数据
     */
    @TableField(value = "is_default")
    private Integer isDefault;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}