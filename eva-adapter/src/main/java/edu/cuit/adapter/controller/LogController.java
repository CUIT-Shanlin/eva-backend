package edu.cuit.adapter.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.log.LogModuleCO;
import edu.cuit.client.dto.clientobject.log.OperateLogCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统日志相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/logs")
public class LogController {

    /**
     * 分页获取日志
     * @param query 分页查询模型
     */
    @PostMapping("/{moduleId}")
    @SaCheckPermission("system.log.query")
    public CommonResult<PaginationQueryResultCO<OperateLogCO>> page(@RequestBody @Valid PagingQuery<GenericConditionalQuery> query,
                                                                    @PathVariable("moduleId") Integer moduleId) {
        return null;
    }

    /**
     * 获取全部日志模块
     */
    @PostMapping("/modules")
    @SaCheckPermission("system.log.query")
    public CommonResult<List<LogModuleCO>> getModules() {
        return null;
    }

}
