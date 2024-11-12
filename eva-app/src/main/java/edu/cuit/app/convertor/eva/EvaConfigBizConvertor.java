package edu.cuit.app.convertor.eva;

import edu.cuit.client.dto.data.EvaConfig;
import edu.cuit.domain.entity.eva.EvaConfigEntity;
import edu.cuit.infra.convertor.EntityFactory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring",uses = EntityFactory.class)
public interface EvaConfigBizConvertor {

    EvaConfig toEvaConfig(EvaConfigEntity configEntity);

}
