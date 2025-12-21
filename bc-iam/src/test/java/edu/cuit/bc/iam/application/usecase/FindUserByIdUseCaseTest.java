package edu.cuit.bc.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cuit.bc.iam.application.port.UserEntityQueryPort;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FindUserByIdUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        FindUserByIdUseCase useCase = new FindUserByIdUseCase(port);

        useCase.execute(10);

        assertEquals(1, port.findByIdCalls);
        assertEquals(10, port.id);
    }

    private static class RecordingPort implements UserEntityQueryPort {
        private int findByIdCalls = 0;
        private Integer id;

        @Override
        public Optional<UserEntity> findById(Integer id) {
            this.findByIdCalls++;
            this.id = id;
            return Optional.empty();
        }

        @Override
        public Optional<UserEntity> findByUsername(String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginationResultEntity<UserEntity> page(edu.cuit.client.dto.query.PagingQuery<edu.cuit.client.dto.query.condition.GenericConditionalQuery> query) {
            throw new UnsupportedOperationException();
        }
    }
}

