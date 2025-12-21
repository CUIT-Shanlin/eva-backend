package edu.cuit.bc.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cuit.bc.iam.application.port.UserMenuCacheInvalidationPort;
import org.junit.jupiter.api.Test;

class HandleUserMenuCacheUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        HandleUserMenuCacheUseCase useCase = new HandleUserMenuCacheUseCase(port);

        useCase.execute(10);

        assertEquals(1, port.calls);
        assertEquals(10, port.menuId);
    }

    private static class RecordingPort implements UserMenuCacheInvalidationPort {
        private int calls = 0;
        private Integer menuId;

        @Override
        public void handleUserMenuCache(Integer menuId) {
            this.calls++;
            this.menuId = menuId;
        }
    }
}

