package edu.cuit.app.convertor.user;

import edu.cuit.client.dto.clientobject.user.MenuCO;
import edu.cuit.domain.entity.user.biz.MenuEntity;
import edu.cuit.infra.convertor.EntityFactory;
import org.mapstruct.*;

/**
 * 菜单业务对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuBizConvertor {

    @Mappings({
            @Mapping(target = "children",ignore = true)
    })
    MenuCO menuEntityToMenuCO(MenuEntity menuEntity);

    @AfterMapping
    default void menuEntity(MenuEntity menuEntity,@MappingTarget MenuCO menuCO) {
        menuCO.setChildren(menuEntity.getChildren().stream().map(this::menuEntityToMenuCO).toList());
    }

}
