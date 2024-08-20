package edu.cuit.client.dto.clientobject.user;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 *总菜单信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class MenuCO extends GenericMenuSectionCO {

    /**
     * 放置子菜单列表
     */
    private List<MenuCO> children;
}
