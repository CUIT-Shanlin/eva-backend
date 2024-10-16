package edu.cuit.infra.convertor;

import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.infra.dal.database.dataobject.course.SemesterDO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SemesterConverter {
    SemesterCO toSemesterCO(SemesterDO semesterDO);

}
