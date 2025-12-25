package edu.cuit.client.dto.clientobject;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 极简响应模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class SimpleResultCO extends ClientObject {

    /**
     * id
     */
    private Integer id;

    /**
     * 名称
     */
    private String name;

}
