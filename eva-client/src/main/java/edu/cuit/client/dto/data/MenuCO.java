package edu.cuit.client.dto.data;

import edu.cuit.client.dto.clientobject.user.SingleMenuCO;
import lombok.Data;

import lombok.experimental.Accessors;

import java.util.List;

/**
 *总菜单信息
 */

@Data
@Accessors(chain = true)
public class MenuCO extends SingleMenuCO {
    //放置子菜单列表
    private List<MenuCO> children;
}
