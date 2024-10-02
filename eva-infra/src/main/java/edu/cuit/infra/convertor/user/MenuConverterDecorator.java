package edu.cuit.infra.convertor.user;

import cn.hutool.extra.spring.SpringUtil;
import edu.cuit.client.dto.clientobject.user.MenuCO;
import edu.cuit.infra.dal.database.dataobject.user.SysMenuDO;
import edu.cuit.infra.dal.database.mapper.user.SysMenuMapper;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class MenuConverterDecorator implements MenuConvertor{

    private MenuConvertor menuConvertor;

    @Override
    public MenuCO toMenuCO(SysMenuDO menuDO) {
        MenuCO menuCO = menuConvertor.toMenuCO(menuDO);
        SysMenuMapper menuMapper = SpringUtil.getBean(SysMenuMapper.class);
        //TODO 查询子菜单
        return menuCO;
    }
}
