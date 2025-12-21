package edu.cuit.bc.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cuit.bc.iam.application.port.UserBasicQueryPort;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FindUserIdByUsernameUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        FindUserIdByUsernameUseCase useCase = new FindUserIdByUsernameUseCase(port);

        useCase.execute("u1");

        assertEquals(1, port.findIdByUsernameCalls);
        assertEquals("u1", port.username);
    }

    private static class RecordingPort implements UserBasicQueryPort {
        private int findIdByUsernameCalls = 0;
        private String username;

        @Override
        public Optional<Integer> findIdByUsername(String username) {
            this.findIdByUsernameCalls++;
            this.username = username;
            return Optional.empty();
        }

        @Override
        public Optional<String> findUsernameById(Integer id) {
            throw new UnsupportedOperationException();
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

