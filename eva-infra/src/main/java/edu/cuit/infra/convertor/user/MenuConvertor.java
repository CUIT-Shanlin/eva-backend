package edu.cuit.infra.convertor.user;

import edu.cuit.client.dto.clientobject.user.MenuCO;
import edu.cuit.domain.entity.user.biz.MenuEntity;
import edu.cuit.infra.convertor.EntityFactory;
import edu.cuit.infra.dal.database.dataobject.user.SysMenuDO;
import org.mapstruct.*;

/**
 * 权限菜单对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class)
public interface MenuConvertor {

    @Mappings({
            @Mapping(target = "children", ignore = true)
    })
    MenuEntity toMenuCO(SysMenuDO menuDO);

    @AfterMapping
    default void afterMenuDOtoMenuCO(SysMenuDO menuDO, @TargetType MenuEntity menuEntity) {
        //TODO 处理子菜单
    }

}
