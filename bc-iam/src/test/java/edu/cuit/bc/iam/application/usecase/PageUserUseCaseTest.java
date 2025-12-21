package edu.cuit.bc.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cuit.bc.iam.application.port.UserEntityQueryPort;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PageUserUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        PageUserUseCase useCase = new PageUserUseCase(port);

        useCase.execute(null);

        assertEquals(1, port.pageCalls);
        assertEquals(null, port.query);
    }

    private static class RecordingPort implements UserEntityQueryPort {
        private int pageCalls = 0;
        private PagingQuery<GenericConditionalQuery> query;

        @Override
        public Optional<UserEntity> findById(Integer id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<UserEntity> findByUsername(String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginationResultEntity<UserEntity> page(PagingQuery<GenericConditionalQuery> query) {
            this.pageCalls++;
            this.query = query;
            return null;
        }
    }
}

