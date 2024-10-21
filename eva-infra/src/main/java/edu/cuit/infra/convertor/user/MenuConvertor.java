package edu.cuit.infra.convertor.user;

import edu.cuit.client.dto.cmd.user.NewMenuCmd;
import edu.cuit.client.dto.cmd.user.UpdateMenuCmd;
import edu.cuit.domain.entity.user.biz.MenuEntity;
import edu.cuit.infra.convertor.EntityFactory;
import edu.cuit.infra.dal.database.dataobject.user.SysMenuDO;
import org.mapstruct.*;

/**
 * 权限菜单对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuConvertor {

    /**
     * 不包含子菜单
     */
    @Mappings({
            @Mapping(target = "children", ignore = true),
            @Mapping(target = "parent", ignore = true)
    })
    MenuEntity toMenuEntity(SysMenuDO menuDO);


    SysMenuDO toMenuDO(UpdateMenuCmd cmd);

    SysMenuDO toMenuDO(NewMenuCmd cmd);
}
