package edu.cuit.domain.gateway;

import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.log.LogModuleCO;
import edu.cuit.client.dto.clientobject.log.OperateLogCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.log.SysLogEntity;
import edu.cuit.domain.entity.log.SysLogModuleEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 日志相关数据门户接口
 */
@Component
public interface LogGateway {

    /**
     * 分页获取日志
     * @param query 分页查询模型
     */
    PaginationResultEntity<SysLogEntity> page(PagingQuery<GenericConditionalQuery> query);

    /**
     * 获取全部日志模块
     */
    List<SysLogModuleEntity> getModules();
}
