package edu.cuit.app.convertor.eva;

import edu.cuit.client.dto.clientobject.user.RoleInfoCO;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.infra.convertor.EntityFactory;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * 评教记录对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EvaRecordBizConvertor {

}
