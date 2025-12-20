package edu.cuit.bc.messaging.application.usecase;

import edu.cuit.bc.messaging.application.port.MessageQueryPort;
import edu.cuit.domain.entity.MsgEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryMessageUseCaseTest {

    @Test
    void queryMsg_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        QueryMessageUseCase useCase = new QueryMessageUseCase(port);

        List<MsgEntity> expected = List.of();
        port.queryMsgResult = expected;

        List<MsgEntity> result = useCase.queryMsg(1, 2, 3);

        assertEquals(1, port.queryMsgCalls);
        assertEquals(1, port.userId);
        assertEquals(2, port.type);
        assertEquals(3, port.mode);
        assertEquals(expected, result);
    }

    @Test
    void queryTargetAmountMsg_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        QueryMessageUseCase useCase = new QueryMessageUseCase(port);

        List<MsgEntity> expected = List.of();
        port.queryTargetAmountMsgResult = expected;

        List<MsgEntity> result = useCase.queryTargetAmountMsg(1, 10, 2);

        assertEquals(1, port.queryTargetAmountMsgCalls);
        assertEquals(1, port.userId);
        assertEquals(10, port.num);
        assertEquals(2, port.type);
        assertEquals(expected, result);
    }

    private static class RecordingPort implements MessageQueryPort {
        private int queryMsgCalls = 0;
        private int queryTargetAmountMsgCalls = 0;
        private Integer userId;
        private Integer type;
        private Integer mode;
        private Integer num;
        private List<MsgEntity> queryMsgResult;
        private List<MsgEntity> queryTargetAmountMsgResult;

        @Override
        public List<MsgEntity> queryMsg(Integer userId, Integer type, Integer mode) {
            this.queryMsgCalls++;
            this.userId = userId;
            this.type = type;
            this.mode = mode;
            return queryMsgResult;
        }

        @Override
        public List<MsgEntity> queryTargetAmountMsg(Integer userId, Integer num, Integer type) {
            this.queryTargetAmountMsgCalls++;
            this.userId = userId;
            this.num = num;
            this.type = type;
            return queryTargetAmountMsgResult;
        }
    }
}

