package edu.cuit.domain.entity.user.biz;

import com.alibaba.cola.domain.Entity;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色domain entity
 */
@Entity
@Data
@RequiredArgsConstructor
public class RoleEntity {

    /**
     * 角色id
     */
    private Integer id;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 描述
     */
    private String description;

    /**
     * 角色权限菜单列表
     */
    private List<MenuEntity> menus;

    /**
     * 状态(0:禁止,1:正常)
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 实现逻辑删除（0:不可用 1:可用）
     */
    private Integer isDeleted;

    /**
     * 逻辑删除
     */
    public Boolean isDeleted() {
        return isDeleted == 1;
    }

}
