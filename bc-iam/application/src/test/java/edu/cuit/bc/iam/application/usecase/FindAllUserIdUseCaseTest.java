package edu.cuit.bc.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cuit.bc.iam.application.port.UserDirectoryQueryPort;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import java.util.List;
import org.junit.jupiter.api.Test;

class FindAllUserIdUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        FindAllUserIdUseCase useCase = new FindAllUserIdUseCase(port);

        useCase.execute();

        assertEquals(1, port.findAllUserIdCalls);
    }

    private static class RecordingPort implements UserDirectoryQueryPort {
        private int findAllUserIdCalls = 0;

        @Override
        public List<Integer> findAllUserId() {
            findAllUserIdCalls++;
            return List.of();
        }

        @Override
        public List<String> findAllUsername() {
            throw new UnsupportedOperationException();
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

