package edu.cuit.infra.convertor.user;

import cn.hutool.extra.spring.SpringUtil;
import edu.cuit.client.dto.clientobject.user.MenuCO;
import edu.cuit.infra.dal.database.dataobject.user.SysMenuDO;
import edu.cuit.infra.dal.database.mapper.user.SysMenuMapper;

public class MenuConverterDecorator implements MenuConvertor{

    private final MenuConvertor menuConvertor;

    public MenuConverterDecorator(MenuConvertor menuConvertor) {
        this.menuConvertor = menuConvertor;
    }

    @Override
    public MenuCO toMenuCO(SysMenuDO menuDO) {
        MenuCO menuCO = menuConvertor.toMenuCO(menuDO);
        SysMenuMapper menuMapper = SpringUtil.getBean(SysMenuMapper.class);
        //TODO 查询子菜单
        return menuCO;
    }
}
