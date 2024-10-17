package edu.cuit.app.convertor.eva;

import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.domain.entity.eva.EvaTemplateEntity;
import edu.cuit.infra.convertor.EntityFactory;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",uses = EntityFactory.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EvaTemplateBizConvertor {
    EvaTemplateCO evaTemplateToEvaTemplateEntity(EvaTemplateEntity evaTemplateEntity);
}
