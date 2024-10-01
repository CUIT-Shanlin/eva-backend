package edu.cuit.infra.convertor.user;

import edu.cuit.client.dto.clientobject.user.MenuCO;
import edu.cuit.infra.convertor.EntityFactory;
import edu.cuit.infra.dal.database.dataobject.user.SysMenuDO;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;

/**
 * 权限菜单对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class)
@DecoratedWith(MenuConverterDecorator.class)
public interface MenuConvertor {

    MenuCO toMenuCO(SysMenuDO menuDO);

}
