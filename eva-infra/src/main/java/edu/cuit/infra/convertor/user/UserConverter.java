package edu.cuit.infra.convertor.user;

import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.domain.entity.user.biz.MenuEntity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.infra.convertor.EntityFactory;
import edu.cuit.infra.dal.database.dataobject.user.SysMenuDO;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * 用户对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class)
public interface UserConverter {

    @Mappings({
        @Mapping(source = "menus",target = "menus"),
        @Mapping(source = "roles",target = "roles")
    })
    UserEntity toUserEntity(SysUserDO userDO, List<RoleEntity> roles, List<MenuEntity> menus);

    RoleEntity toRoleEntity(SysRoleDO roleDO);

    MenuEntity toMenuEntity(SysMenuDO menuDO);

    SimpleResultCO toUserSimpleResult(SysUserDO userDO);

    @Mappings({
            @Mapping(source = "finishedEvaNum", target = "num")
    })
    UnqualifiedUserInfoCO toUnqualifiedUserInfo(SysUserDO userDO,Integer finishedEvaNum);
}
