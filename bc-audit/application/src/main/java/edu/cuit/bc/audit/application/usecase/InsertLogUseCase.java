package edu.cuit.bc.audit.application.usecase;

import edu.cuit.bc.audit.application.port.LogInsertionPort;
import edu.cuit.client.bo.SysLogBO;

import java.util.Objects;

/**
 * 插入审计日志用例（写模型入口）。
 *
 * <p>保持行为不变：用例仅做编排与依赖隔离，具体落库与字段补齐逻辑在端口适配器中原样搬运。</p>
 */
public class InsertLogUseCase {
    private final LogInsertionPort insertionPort;

    public InsertLogUseCase(LogInsertionPort insertionPort) {
        this.insertionPort = Objects.requireNonNull(insertionPort, "insertionPort");
    }

    public void insertLog(SysLogBO logBO) {
        insertionPort.insertLog(logBO);
    }
}

