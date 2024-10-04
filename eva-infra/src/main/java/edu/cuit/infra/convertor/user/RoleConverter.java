package edu.cuit.infra.convertor.user;

import edu.cuit.client.dto.clientobject.user.SimpleRoleInfoCO;
import edu.cuit.domain.entity.user.biz.MenuEntity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.infra.convertor.EntityFactory;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * 角色对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class)
public interface RoleConverter {

    /**
     * 不携带菜单列表
     */
    @Mappings({
            @Mapping(target = "menus",ignore = true)
    })
    RoleEntity toRoleEntity(SysRoleDO roleDO);

    @Mappings({
            @Mapping(target = "menus",source = "menus")
    })
    RoleEntity toRoleEntity(SysRoleDO roleDO,List<MenuEntity> menus);


    @Mapping(target = "extValues", ignore = true)
    SimpleRoleInfoCO toSimpleRoleInfo(SysRoleDO roleDO);

}
