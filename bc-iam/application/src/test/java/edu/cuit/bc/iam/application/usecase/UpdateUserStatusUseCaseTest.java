package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.UserStatusUpdatePort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateUserStatusUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        UpdateUserStatusUseCase useCase = new UpdateUserStatusUseCase(port);

        useCase.execute(10, 0);

        assertEquals(1, port.calls);
        assertEquals(10, port.userId);
        assertEquals(0, port.status);
    }

    private static class RecordingPort implements UserStatusUpdatePort {
        private int calls = 0;
        private Integer userId;
        private Integer status;

        @Override
        public void updateStatus(Integer userId, Integer status) {
            this.calls++;
            this.userId = userId;
            this.status = status;
        }
    }
}

