package edu.cuit.bc.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cuit.bc.iam.application.port.UserDirectoryQueryPort;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import java.util.List;
import org.junit.jupiter.api.Test;

class FindAllUsernameUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        FindAllUsernameUseCase useCase = new FindAllUsernameUseCase(port);

        useCase.execute();

        assertEquals(1, port.findAllUsernameCalls);
    }

    private static class RecordingPort implements UserDirectoryQueryPort {
        private int findAllUsernameCalls = 0;

        @Override
        public List<Integer> findAllUserId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> findAllUsername() {
            findAllUsernameCalls++;
            return List.of();
        }

        @Override
        public List<SimpleResultCO> allUser() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Integer> getUserRoleIds(Integer userId) {
            throw new UnsupportedOperationException();
        }
    }
}

