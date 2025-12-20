package edu.cuit.bc.messaging.application.usecase;

import edu.cuit.bc.messaging.application.port.MessageInsertionPort;
import edu.cuit.client.dto.data.msg.GenericRequestMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InsertMessageUseCaseTest {

    @Test
    void insertMessage_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        InsertMessageUseCase useCase = new InsertMessageUseCase(port);

        GenericRequestMsg msg = new GenericRequestMsg();
        useCase.insertMessage(msg);

        assertEquals(1, port.calls);
        assertEquals(msg, port.msg);
    }

    private static class RecordingPort implements MessageInsertionPort {
        private int calls = 0;
        private GenericRequestMsg msg;

        @Override
        public void insertMessage(GenericRequestMsg msg) {
            this.calls++;
            this.msg = msg;
        }
    }
}

