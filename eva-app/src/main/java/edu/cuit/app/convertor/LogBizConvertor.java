package edu.cuit.app.convertor;

import edu.cuit.client.bo.SysLogBO;
import edu.cuit.client.dto.clientobject.log.LogModuleCO;
import edu.cuit.client.dto.clientobject.log.OperateLogCO;
import edu.cuit.domain.entity.log.SysLogEntity;
import edu.cuit.domain.entity.log.SysLogModuleEntity;
import edu.cuit.infra.convertor.EntityFactory;
import edu.cuit.zhuyimeng.framework.logging.aspect.OperateLogBO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;

/**
 * 日志业务类型转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LogBizConvertor {

    @Mappings({
            @Mapping(target = "moduleId", expression = "java(log.getModule().getId())"),
            @Mapping(target = "userName", expression = "java(log.getUser().getName())")
    })
    OperateLogCO toOperateLogCO(SysLogEntity log);

    @Mappings({
            @Mapping(target = "content",source = "logBO.customContent")
    })
    SysLogBO toOperateLogBO(OperateLogBO logBO,Integer moduleId);

    LogModuleCO toLogModuleCO(SysLogModuleEntity module);

}
