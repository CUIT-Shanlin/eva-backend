package edu.cuit.bc.audit.application.port;

import edu.cuit.client.bo.SysLogBO;

/**
 * 审计日志写入端口。
 *
 * <p>保持行为不变：异步触发点仍保留在旧 gateway（委托壳）侧；端口适配器仅原样执行落库与字段补齐逻辑。</p>
 */
public interface LogInsertionPort {
    void insertLog(SysLogBO logBO);
}

