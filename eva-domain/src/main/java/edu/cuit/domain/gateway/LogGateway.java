package edu.cuit.domain.gateway;

import edu.cuit.client.bo.SysLogBO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.log.SysLogEntity;
import edu.cuit.domain.entity.log.SysLogModuleEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 日志相关数据门户接口
 */
@Component
public interface LogGateway {

    /**
     * 分页获取日志
     * @param query 分页查询模型
     * @param moduleId 模块id，为负数则为全部
     */
    PaginationResultEntity<SysLogEntity> page(PagingQuery<GenericConditionalQuery> query,Integer moduleId);

    /**
     * 通过模块名查询模块
     * @param name 模块名
     */
    Optional<SysLogModuleEntity> getModuleByName(String name);

    /**
     * 获取全部日志模块
     */
    List<SysLogModuleEntity> getModules();

    /**
     * 插入日志数据（异步执行）
     * @param logBO 日志BO
     */
    void insertLog(SysLogBO logBO);
}
