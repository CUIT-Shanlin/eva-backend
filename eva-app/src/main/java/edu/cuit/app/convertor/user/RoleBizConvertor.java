package edu.cuit.app.convertor.user;

import edu.cuit.client.dto.clientobject.user.RoleInfoCO;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.infra.convertor.EntityFactory;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * 角色业务对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoleBizConvertor {

    RoleInfoCO roleEntityToRoleInfoCO(RoleEntity entity);

}
