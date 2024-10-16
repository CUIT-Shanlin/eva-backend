package edu.cuit.domain.entity.user.biz;

import com.alibaba.cola.domain.Entity;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

/**
 * 权限菜单domain entity
 */
@Entity
@Data
@RequiredArgsConstructor
public class MenuEntity {

    /**
     * 菜单权限id
     */
    private Integer id;

    @Getter(AccessLevel.NONE)
    private Supplier<List<MenuEntity>> children;

    /**
     * 名称
     */
    private String name;

    /**
     * 类型(0:目录,1:菜单,2:按钮)
     */
    private Integer type;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 权限标识
     */
    private String perms;

    /**
     * 图标unicode码
     */
    private String icon;

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

    @Getter(AccessLevel.NONE)
    private List<MenuEntity> cache = null;

    public synchronized List<MenuEntity> getChildren() {
        if (cache == null) {
            cache = children.get();
        }
        return cache;
    }

}
