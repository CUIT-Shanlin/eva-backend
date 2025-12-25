package edu.cuit.bc.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cuit.bc.iam.application.port.UserDirectoryQueryPort;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import java.util.List;
import org.junit.jupiter.api.Test;

class AllUserUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        AllUserUseCase useCase = new AllUserUseCase(port);

        useCase.execute();

        assertEquals(1, port.allUserCalls);
    }

    private static class RecordingPort implements UserDirectoryQueryPort {
        private int allUserCalls = 0;

        @Override
        public List<Integer> findAllUserId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> findAllUsername() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SimpleResultCO> allUser() {
            allUserCalls++;
            return List.of();
        }

        @Override
        public List<Integer> getUserRoleIds(Integer userId) {
            throw new UnsupportedOperationException();
        }
    }
}

