package edu.cuit.infra.convertor.user;

import edu.cuit.domain.entity.user.biz.MenuEntity;
import edu.cuit.infra.convertor.EntityFactory;
import edu.cuit.infra.dal.database.dataobject.user.SysMenuDO;
import org.mapstruct.*;

/**
 * 权限菜单对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class)
public interface MenuConvertor {

    /**
     * 不包含子菜单
     */
    @Mappings({
            @Mapping(target = "children", ignore = true)
    })
    MenuEntity toMenuEntity(SysMenuDO menuDO);

}
