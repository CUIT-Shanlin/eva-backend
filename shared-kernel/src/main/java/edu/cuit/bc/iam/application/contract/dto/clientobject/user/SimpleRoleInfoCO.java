package edu.cuit.bc.iam.application.contract.dto.clientobject.user;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 极简角色信息模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class SimpleRoleInfoCO extends ClientObject {

    /**
     * 角色id
     */
    private Integer id;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 角色名
     */
    private String roleName;

}
