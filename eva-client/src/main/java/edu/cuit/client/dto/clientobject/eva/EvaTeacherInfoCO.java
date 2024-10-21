package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 老师信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class EvaTeacherInfoCO extends ClientObject {

    /**
     * 老师ID
     */
    private Integer id;

    /**
     * 老师名称
     */
    private String name;

}
