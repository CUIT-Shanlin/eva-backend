package edu.cuit.infra.bcaudit.adapter;

import edu.cuit.bc.audit.application.port.LogInsertionPort;
import edu.cuit.bc.iam.application.port.UserBasicQueryPort;
import edu.cuit.client.bo.SysLogBO;
import edu.cuit.infra.convertor.LogConverter;
import edu.cuit.infra.dal.database.dataobject.log.SysLogDO;
import edu.cuit.infra.dal.database.mapper.log.SysLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 审计日志写入端口适配器（过渡期落在 eva-infra）。
 *
 * <p>保持行为不变：仅原样搬运旧实现中的“字段补齐 + 落库”。</p>
 */
@RequiredArgsConstructor
@Component
public class LogInsertionPortImpl implements LogInsertionPort {
    private final SysLogMapper logMapper;
    private final LogConverter logConverter;
    private final UserBasicQueryPort userBasicQueryPort;

    @Override
    public void insertLog(SysLogBO logBO) {
        SysLogDO logDO = logConverter.toLogDO(logBO, userBasicQueryPort.findIdByUsername(logBO.getUserId()).orElse(null));
        logMapper.insert(logDO);
    }
}
