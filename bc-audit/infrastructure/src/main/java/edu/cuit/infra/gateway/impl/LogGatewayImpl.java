package edu.cuit.infra.gateway.impl;

import com.alibaba.cola.exception.SysException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.cuit.bc.audit.application.usecase.InsertLogUseCase;
import edu.cuit.bc.iam.application.port.UserEntityObjectByIdDirectQueryPort;
import edu.cuit.client.bo.SysLogBO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.log.SysLogEntity;
import edu.cuit.domain.entity.log.SysLogModuleEntity;
import edu.cuit.domain.gateway.LogGateway;
import edu.cuit.infra.convertor.LogConverter;
import edu.cuit.infra.convertor.PaginationConverter;
import edu.cuit.infra.dal.database.dataobject.log.SysLogDO;
import edu.cuit.infra.dal.database.dataobject.log.SysLogModuleDO;
import edu.cuit.infra.dal.database.mapper.log.SysLogMapper;
import edu.cuit.infra.dal.database.mapper.log.SysLogModuleMapper;
import edu.cuit.infra.util.QueryUtils;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RequiredArgsConstructor
@Component
@Slf4j
public class LogGatewayImpl implements LogGateway {
    private final SysLogMapper logMapper;
    private final UserEntityObjectByIdDirectQueryPort userEntityObjectByIdDirectQueryPort;
    private final SysLogModuleMapper logModuleMapper;
    private final LogConverter logConverter;
    private final PaginationConverter pageConverter;

    private final InsertLogUseCase insertLogUseCase;

    private final Executor executor;

    @Override
    public PaginationResultEntity<SysLogEntity> page(PagingQuery<GenericConditionalQuery> query, Integer moduleId) {
        QueryWrapper<SysLogDO> wrapper = new QueryWrapper<>();
        QueryUtils.fileCreateTimeQuery(wrapper, query.getQueryObj());
        if (query.getQueryObj().getKeyword() != null) {
            wrapper.like("content", query.getQueryObj().getKeyword());
        }
        Page<SysLogDO> page = new Page<>(query.getPage(), query.getSize());
        if (moduleId >= 0) {
            wrapper.eq("module_id", moduleId);
        }
        wrapper.orderByDesc("create_time");
        Page<SysLogDO> logPage = logMapper.selectPage(page, wrapper);
        List<SysLogDO> records = logPage.getRecords();
        List<SysLogEntity> logEntities = toSysLogEntityList(records);
        return pageConverter.toPaginationEntity(logPage, logEntities);
    }

    @Override
    public Optional<SysLogModuleEntity> getModuleByName(String name) {
        LambdaQueryWrapper<SysLogModuleDO> moduleQuery = Wrappers.lambdaQuery();
        moduleQuery.eq(SysLogModuleDO::getName, name);
        SysLogModuleDO sysLogModuleDO = logModuleMapper.selectOne(moduleQuery);
        if (sysLogModuleDO == null) {
            SysException e = new SysException(name + " 日志模块不存在，请联系管理员");
            log.error("发生系统异常", e);
            throw e;
        }
        return Optional.of(logConverter.toModuleEntity(sysLogModuleDO));
    }

    @Override
    public List<SysLogModuleEntity> getModules() {
        List<SysLogModuleDO> sysLogModuleDOS = logModuleMapper.selectList(null);
        return sysLogModuleDOS.stream().map(logConverter::toModuleEntity).toList();
    }

    @Override
    public void insertLog(SysLogBO logBO) {
        CompletableFuture.runAsync(() -> insertLogUseCase.insertLog(logBO), executor);
    }

    @Override
    public void clearOldLog() {
        LambdaQueryWrapper<SysLogDO> logQuery = Wrappers.lambdaQuery();
        logQuery.lt(SysLogDO::getCreateTime, LocalDateTime.now().minusWeeks(1));
        logMapper.delete(logQuery);
    }

    private SysLogEntity toSysLogEntity(SysLogDO logDO) {
        SysLogModuleDO sysLogModuleDO = logModuleMapper.selectById(logDO.getModuleId());
        if (sysLogModuleDO == null) throw new QueryException("日志模块不存在");
        SysLogModuleEntity moduleEntity = logConverter.toModuleEntity(sysLogModuleDO);
        Object userEntity = userEntityObjectByIdDirectQueryPort.findById(logDO.getUserId());
        return logConverter.toLogEntityWithUserObject(logDO, moduleEntity, userEntity);

    }

    private List<SysLogEntity> toSysLogEntityList(List<SysLogDO> records) {
        List<SysLogEntity> logEntities = new ArrayList<>();
        for (SysLogDO record : records) {
            logEntities.add(toSysLogEntity(record));
        }
        return logEntities;
    }
}
