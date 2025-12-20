package edu.cuit.bc.messaging.application.usecase;

import edu.cuit.bc.messaging.application.port.MessageInsertionPort;
import edu.cuit.client.dto.data.msg.GenericRequestMsg;

import java.util.Objects;

/**
 * 新增消息用例（写模型入口）。
 *
 * <p>保持行为不变：用例仅做编排与依赖隔离，具体落库与字段补齐逻辑在端口适配器中原样搬运。</p>
 */
public class InsertMessageUseCase {
    private final MessageInsertionPort insertionPort;

    public InsertMessageUseCase(MessageInsertionPort insertionPort) {
        this.insertionPort = Objects.requireNonNull(insertionPort, "insertionPort");
    }

    public void insertMessage(GenericRequestMsg msg) {
        insertionPort.insertMessage(msg);
    }
}

