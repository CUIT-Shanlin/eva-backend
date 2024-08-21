package edu.cuit.client.dto.clientobject.log;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 系统日志模块
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class LogModuleCO extends ClientObject {

    /**
     * id
     */
    private Long id;

    /**
     * 模块名称
     */
    private String name;

}
