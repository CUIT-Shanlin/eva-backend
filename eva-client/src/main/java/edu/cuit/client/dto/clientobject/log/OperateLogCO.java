package edu.cuit.client.dto.clientobject.log;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 操作日志
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class OperateLogCO extends ClientObject {

    /**
     * id
     */
    private Integer id;

    /**
     * 模块ID
     */
    private Integer moduleId;

    /**
     * 操作类型, 0123 对应 增删改查，4是其他
     */
    private Integer type;

    /**
     * 操作者姓名
     */
    private String userName;

    /**
     * 操作内容
     */
    private String content;

    /**
     * 操作者IP
     */
    private String ip;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
