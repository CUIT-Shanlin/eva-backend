package edu.cuit.bc.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cuit.bc.iam.application.port.UserEntityQueryPort;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FindUserByUsernameUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        FindUserByUsernameUseCase useCase = new FindUserByUsernameUseCase(port);

        useCase.execute("alice");

        assertEquals(1, port.findByUsernameCalls);
        assertEquals("alice", port.username);
    }

    private static class RecordingPort implements UserEntityQueryPort {
        private int findByUsernameCalls = 0;
        private String username;

        @Override
        public Optional<UserEntity> findById(Integer id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<UserEntity> findByUsername(String username) {
            this.findByUsernameCalls++;
            this.username = username;
            return Optional.empty();
        }

        @Override
        public PaginationResultEntity<UserEntity> page(edu.cuit.client.dto.query.PagingQuery<edu.cuit.client.dto.query.condition.GenericConditionalQuery> query) {
            throw new UnsupportedOperationException();
        }
    }
}

