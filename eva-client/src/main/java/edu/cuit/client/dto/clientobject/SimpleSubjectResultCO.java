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

public class SimpleSubjectResultCO extends ClientObject {

    /**
     * id
     */
    private Integer id;

    /**
     * 名称
     */
    private String name;

    /**
     * 课程性质(0:理论课,1:实验课,3:其他)
     * */
    private Integer nature;
}
