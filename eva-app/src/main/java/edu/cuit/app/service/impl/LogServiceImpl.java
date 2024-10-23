package edu.cuit.app.service.impl;

import com.alibaba.cola.exception.SysException;
import edu.cuit.app.convertor.LogBizConvertor;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.client.api.ILogService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.log.LogModuleCO;
import edu.cuit.client.dto.clientobject.log.OperateLogCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.log.SysLogEntity;
import edu.cuit.domain.entity.log.SysLogModuleEntity;
import edu.cuit.domain.gateway.LogGateway;
import edu.cuit.zhuyimeng.framework.logging.aspect.LogManager;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogServiceImpl implements ILogService {

    private final LogGateway logGateway;

    private final LogBizConvertor logBizConvertor;
    private final PaginationBizConvertor paginationBizConvertor;

    @PostConstruct
    public void registerListener() {
        // 注册日志监听器
        LogManager.getInstance().register(operateLogBO -> {
            logGateway.insertLog(logBizConvertor.toOperateLogBO(operateLogBO,
                    logGateway.getModuleByName(operateLogBO.getModule())
                            .map(SysLogModuleEntity::getId)
                            .orElseThrow(() -> {
                                SysException e = new SysException("日志记录失败：日志模块 " + operateLogBO.getModule() + " 不存在");
                                log.error("发生系统异常",e);
                                return e;
                            })));
        });
    }

    @Override
    @Transactional
    public PaginationQueryResultCO<OperateLogCO> page(PagingQuery<GenericConditionalQuery> query
            , Integer moduleId) {
        PaginationResultEntity<SysLogEntity> resultPage = logGateway.page(query, moduleId);
        List<SysLogEntity> records = resultPage.getRecords();
        return paginationBizConvertor.toPaginationEntity(resultPage,records.stream()
                .map(logBizConvertor::toOperateLogCO)
                .toList());
    }

    @Override
    @Transactional
    public List<LogModuleCO> getModules() {
        return logGateway.getModules().stream()
                .map(logBizConvertor::toLogModuleCO)
                .toList();
    }
}
