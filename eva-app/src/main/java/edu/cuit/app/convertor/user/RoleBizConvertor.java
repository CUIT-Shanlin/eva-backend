package edu.cuit.app.convertor.user;

import edu.cuit.client.dto.clientobject.user.RoleInfoCO;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.infra.convertor.EntityFactory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",uses = EntityFactory.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoleBizConvertor {

    RoleInfoCO roleEntityToRoleDO(RoleEntity entity);

}
