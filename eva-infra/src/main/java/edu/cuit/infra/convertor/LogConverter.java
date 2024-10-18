package edu.cuit.infra.convertor;

import edu.cuit.client.bo.SysLogBO;
import edu.cuit.domain.entity.log.SysLogEntity;
import edu.cuit.domain.entity.log.SysLogModuleEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.infra.dal.database.dataobject.log.SysLogDO;
import edu.cuit.infra.dal.database.dataobject.log.SysLogModuleDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LogConverter {
   @Mappings({
           @Mapping(target = "id",source = "logDO.id"),
           @Mapping(target = "module",source = "module"),
           @Mapping(target = "type",source = "logDO.type"),
           @Mapping(target = "user",source = "user"),
           @Mapping(target = "ip",source = "logDO.ip"),
           @Mapping(target = "content",source = "logDO.content"),
           @Mapping(target = "createTime",source = "logDO.createTime"),
   })
   SysLogEntity toLogEntity(SysLogDO logDO, SysLogModuleEntity module, UserEntity user);

   SysLogModuleEntity toModuleEntity(SysLogModuleDO moduleDO);

   SysLogDO toLogDO(SysLogBO logBO);
}
