package edu.cuit.bc.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cuit.bc.iam.application.port.UserBasicQueryPort;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetUserStatusUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        GetUserStatusUseCase useCase = new GetUserStatusUseCase(port);

        useCase.execute(10);

        assertEquals(1, port.getUserStatusCalls);
        assertEquals(10, port.id);
    }

    private static class RecordingPort implements UserBasicQueryPort {
        private int getUserStatusCalls = 0;
        private Integer id;

        @Override
        public Optional<Integer> findIdByUsername(String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<String> findUsernameById(Integer id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Integer> getUserStatus(Integer id) {
            this.getUserStatusCalls++;
            this.id = id;
            return Optional.empty();
        }

        @Override
        public Boolean isUsernameExist(String username) {
            throw new UnsupportedOperationException();
        }
    }
}

