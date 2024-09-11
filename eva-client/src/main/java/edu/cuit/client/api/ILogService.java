package edu.cuit.client.api;

import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.log.LogModuleCO;
import edu.cuit.client.dto.clientobject.log.OperateLogCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 系统日志相关接口
 */
public interface ILogService {

    /**
     * 分页获取日志
     * @param query 分页查询模型
     */
    PaginationQueryResultCO<OperateLogCO> page(PagingQuery<GenericConditionalQuery> query);

    /**
     * 获取全部日志模块
     */
    List<LogModuleCO> getModules();
}
