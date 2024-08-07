package edu.cuit.infra.dal.po.log;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 系统日志表
 * @TableName sys_log
 */
@TableName(value ="sys_log")
@Data
public class SysLogDO implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 模块ID
     */
    @TableField(value = "moduleId")
    private Integer moduleId;

    /**
     * 操作类型, 0123 对应 增删改查，4是其他
     */
    @TableField(value = "type")
    private Integer type;

    /**
     * 操作者ID
     */
    @TableField(value = "userId")
    private Integer userId;

    /**
     * 操作者IP
     */
    @TableField(value = "ip")
    private String ip;

    /**
     * 操作内容
     */
    @TableField(value = "content")
    private String content;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime create_time;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}