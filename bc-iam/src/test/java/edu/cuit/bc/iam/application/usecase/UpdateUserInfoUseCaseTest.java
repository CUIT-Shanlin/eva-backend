package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.UserInfoUpdatePort;
import edu.cuit.client.dto.cmd.user.UpdateUserCmd;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class UpdateUserInfoUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        UpdateUserInfoUseCase useCase = new UpdateUserInfoUseCase(port);

        UpdateUserCmd cmd = new UpdateUserCmd();
        useCase.execute(cmd);

        assertSame(cmd, port.receivedCmd);
    }

    private static class RecordingPort implements UserInfoUpdatePort {
        private UpdateUserCmd receivedCmd;

        @Override
        public void updateInfo(UpdateUserCmd cmd) {
            this.receivedCmd = cmd;
        }
    }
}

