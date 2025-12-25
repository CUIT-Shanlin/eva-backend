package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.UserCreationPort;
import edu.cuit.client.dto.cmd.user.NewUserCmd;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class CreateUserUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        CreateUserUseCase useCase = new CreateUserUseCase(port);

        NewUserCmd cmd = new NewUserCmd();
        useCase.execute(cmd);

        assertSame(cmd, port.receivedCmd);
    }

    private static class RecordingPort implements UserCreationPort {
        private NewUserCmd receivedCmd;

        @Override
        public void createUser(NewUserCmd cmd) {
            this.receivedCmd = cmd;
        }
    }
}

