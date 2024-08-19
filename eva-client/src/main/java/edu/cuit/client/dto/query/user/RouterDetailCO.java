package edu.cuit.client.dto.query.user;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 路由数据模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class RouterDetailCO extends ClientObject {

    /**
     * 路由地址
     */
    private String path;

    /**
     * 是否一直显示，用于确定该路由对应的是菜单、目录还是按钮级别
     */
    private Boolean alwaysShow;

    /**
     * 放子菜单列表
     */
    private List<RouterDetailCO> children;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 是否隐藏路由
     */
    private Boolean hidden;

    /**
     * 其他数据
     */
    private RouterMeta meta;

}
