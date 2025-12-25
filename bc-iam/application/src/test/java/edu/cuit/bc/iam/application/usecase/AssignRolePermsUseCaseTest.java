package edu.cuit.bc.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cuit.bc.iam.application.port.RolePermissionAssignmentPort;
import java.util.List;
import org.junit.jupiter.api.Test;

class AssignRolePermsUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        AssignRolePermsUseCase useCase = new AssignRolePermsUseCase(port);

        useCase.execute(10, List.of(1, 2, 3));

        assertEquals(1, port.calls);
        assertEquals(10, port.roleId);
        assertEquals(List.of(1, 2, 3), port.menuIds);
    }

    private static class RecordingPort implements RolePermissionAssignmentPort {
        private int calls = 0;
        private Integer roleId;
        private List<Integer> menuIds;

        @Override
        public void assignPerms(Integer roleId, List<Integer> menuIds) {
            this.calls++;
            this.roleId = roleId;
            this.menuIds = menuIds;
        }
    }
}

