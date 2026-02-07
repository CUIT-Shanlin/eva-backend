package edu.cuit.infra.dal.database.dataobject.log;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 系统日志模块表
 * @TableName sys_log_module
 */
@TableName(value ="sys_log_module")
@Data
public class SysLogModuleDO implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 模块名称
     */
    @TableField(value = "name")
    private String name;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}