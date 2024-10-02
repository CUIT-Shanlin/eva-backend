package edu.cuit.infra.convertor.user;

import edu.cuit.client.dto.clientobject.user.MenuCO;
import edu.cuit.infra.convertor.EntityFactory;
import edu.cuit.infra.dal.database.dataobject.user.SysMenuDO;
import org.mapstruct.AfterMapping;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.TargetType;

/**
 * 权限菜单对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class)
public interface MenuConvertor {

    MenuCO toMenuCO(SysMenuDO menuDO);

    @AfterMapping
    default void afterMenuDOtoMenuCO(SysMenuDO menuDO, @TargetType MenuCO menuCO) {
        //TODO 处理子菜单
    }

}
