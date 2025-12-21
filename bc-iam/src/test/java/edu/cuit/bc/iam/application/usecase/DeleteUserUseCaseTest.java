package edu.cuit.bc.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cuit.bc.iam.application.port.UserDeletionPort;
import org.junit.jupiter.api.Test;

class DeleteUserUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        DeleteUserUseCase useCase = new DeleteUserUseCase(port);

        useCase.execute(10);

        assertEquals(1, port.calls);
        assertEquals(10, port.userId);
    }

    private static class RecordingPort implements UserDeletionPort {
        private int calls = 0;
        private Integer userId;

        @Override
        public void deleteUser(Integer userId) {
            this.calls++;
            this.userId = userId;
        }
    }
}

