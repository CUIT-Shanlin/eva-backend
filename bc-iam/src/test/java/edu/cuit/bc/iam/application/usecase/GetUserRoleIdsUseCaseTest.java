package edu.cuit.bc.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cuit.bc.iam.application.port.UserDirectoryQueryPort;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetUserRoleIdsUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        GetUserRoleIdsUseCase useCase = new GetUserRoleIdsUseCase(port);

        useCase.execute(10);

        assertEquals(1, port.getUserRoleIdsCalls);
        assertEquals(10, port.userId);
    }

    private static class RecordingPort implements UserDirectoryQueryPort {
        private int getUserRoleIdsCalls = 0;
        private Integer userId;

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
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Integer> getUserRoleIds(Integer userId) {
            getUserRoleIdsCalls++;
            this.userId = userId;
            return List.of();
        }
    }
}

