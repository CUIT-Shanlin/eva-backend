package edu.cuit.app.service.impl;

import edu.cuit.client.api.ILogService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.log.LogModuleCO;
import edu.cuit.client.dto.clientobject.log.OperateLogCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogServiceImpl implements ILogService {

    @Override
    public PaginationQueryResultCO<OperateLogCO> page(PagingQuery<GenericConditionalQuery> query
            , Integer moduleId) {
        return null;
    }

    @Override
    public List<LogModuleCO> getModules() {
        return List.of();
    }
}
