package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.UserRoleAssignmentPort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssignRoleUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        AssignRoleUseCase useCase = new AssignRoleUseCase(port);

        useCase.execute(10, List.of(1, 2, 2));

        assertEquals(1, port.calls);
        assertEquals(10, port.userId);
        assertEquals(List.of(1, 2, 2), port.roleId);
    }

    private static class RecordingPort implements UserRoleAssignmentPort {
        private int calls = 0;
        private Integer userId;
        private List<Integer> roleId;

        @Override
        public void assignRole(Integer userId, List<Integer> roleId) {
            this.calls++;
            this.userId = userId;
            this.roleId = roleId;
        }
    }
}

