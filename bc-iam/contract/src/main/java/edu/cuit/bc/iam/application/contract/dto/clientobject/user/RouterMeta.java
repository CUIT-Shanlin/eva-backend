package edu.cuit.bc.iam.application.contract.dto.clientobject.user;

import com.alibaba.cola.dto.DTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 路由元素数据
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class RouterMeta extends DTO {

    /**
     * 路由显示名称
     */
    private String name;

    /**
     * 路由图标
     */
    private String icon;

}
