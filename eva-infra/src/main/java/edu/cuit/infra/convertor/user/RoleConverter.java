package edu.cuit.infra.convertor.user;

import edu.cuit.client.dto.clientobject.user.SimpleRoleInfoCO;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.infra.convertor.EntityFactory;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import org.mapstruct.Mapper;

/**
 * 角色对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class)
public interface RoleConverter {

    RoleEntity toRoleEntity(SysRoleDO roleDO);

    SimpleRoleInfoCO toSimpleRoleInfo(SysRoleDO roleDO);

}
