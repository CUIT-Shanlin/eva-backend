package edu.cuit.bc.iam.application.contract.dto.clientobject.user;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class

RoleInfoCO extends ClientObject {

    /**
     * 角色id
     */
    private Long id;

    /**
     * 描述
     */
    private String description;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 状态(0:禁止,1:正常)
     */
    private Long status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
