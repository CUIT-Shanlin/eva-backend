package edu.cuit.infra.dal.database.dataobject.course;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 学期表
 * @TableName semester
 */
@TableName(value ="semester")
@Data
public class SemesterDO implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 开始年份，如2023
     */
    @TableField(value = "start_year")
    private String start_year;

    /**
     * 上下学期，0为上，1为下
     */
    @TableField(value = "period")
    private Integer period;

    /**
     * 逻辑删除
     */
    @TableField(value = "is_deleted")
    private Integer is_deleted;

    /**
     * 结束年份
     */
    @TableField(value = "end_year")
    private String end_year;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}