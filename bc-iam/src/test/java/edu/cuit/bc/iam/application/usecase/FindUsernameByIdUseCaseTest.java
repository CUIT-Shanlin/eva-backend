package edu.cuit.bc.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cuit.bc.iam.application.port.UserBasicQueryPort;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FindUsernameByIdUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        FindUsernameByIdUseCase useCase = new FindUsernameByIdUseCase(port);

        useCase.execute(10);

        assertEquals(1, port.findUsernameByIdCalls);
        assertEquals(10, port.id);
    }

    private static class RecordingPort implements UserBasicQueryPort {
        private int findUsernameByIdCalls = 0;
        private Integer id;

        @Override
        public Optional<Integer> findIdByUsername(String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<String> findUsernameById(Integer id) {
            this.findUsernameByIdCalls++;
            this.id = id;
            return Optional.empty();
        }

        @Override
        public Optional<Integer> getUserStatus(Integer id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Boolean isUsernameExist(String username) {
            throw new UnsupportedOperationException();
        }
    }
}

