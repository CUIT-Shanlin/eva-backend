package edu.cuit.bc.messaging.application.usecase;

import edu.cuit.bc.messaging.application.port.MessageDisplayPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateMessageDisplayUseCaseTest {

    @Test
    void updateDisplay_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        UpdateMessageDisplayUseCase useCase = new UpdateMessageDisplayUseCase(port);

        useCase.updateDisplay(1, 2, 1);

        assertEquals(1, port.calls);
        assertEquals(1, port.userId);
        assertEquals(2, port.id);
        assertEquals(1, port.isDisplayed);
    }

    private static class RecordingPort implements MessageDisplayPort {
        private int calls = 0;
        private Integer userId;
        private Integer id;
        private Integer isDisplayed;

        @Override
        public void updateDisplay(Integer userId, Integer id, Integer isDisplayed) {
            this.calls++;
            this.userId = userId;
            this.id = id;
            this.isDisplayed = isDisplayed;
        }
    }
}

