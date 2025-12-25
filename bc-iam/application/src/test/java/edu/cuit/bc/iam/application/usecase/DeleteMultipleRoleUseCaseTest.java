package edu.cuit.bc.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cuit.bc.iam.application.port.RoleBatchDeletionPort;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeleteMultipleRoleUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        DeleteMultipleRoleUseCase useCase = new DeleteMultipleRoleUseCase(port);

        useCase.execute(List.of(1, 2, 3));

        assertEquals(1, port.calls);
        assertEquals(List.of(1, 2, 3), port.roleIds);
    }

    private static class RecordingPort implements RoleBatchDeletionPort {
        private int calls = 0;
        private List<Integer> roleIds;

        @Override
        public void deleteMultipleRole(List<Integer> roleIds) {
            this.calls++;
            this.roleIds = roleIds;
        }
    }
}

