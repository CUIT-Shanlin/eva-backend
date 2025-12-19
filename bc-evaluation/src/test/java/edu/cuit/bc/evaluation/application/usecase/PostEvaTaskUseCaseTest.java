package edu.cuit.bc.evaluation.application.usecase;

import edu.cuit.bc.evaluation.application.model.PostEvaTaskCommand;
import edu.cuit.bc.evaluation.application.port.DomainEventPublisher;
import edu.cuit.bc.evaluation.application.port.PostEvaTaskRepository;
import edu.cuit.bc.evaluation.domain.event.DomainEvent;
import edu.cuit.bc.evaluation.domain.event.EvaluationTaskPostedEvent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PostEvaTaskUseCaseTest {

    @Test
    void post_whenOk_shouldCreateAndPublish() {
        FakeRepo repo = new FakeRepo(123);
        FakePublisher publisher = new FakePublisher();
        PostEvaTaskUseCase useCase = new PostEvaTaskUseCase(repo, publisher);

        Integer taskId = useCase.post(new PostEvaTaskCommand(10, 20), 999);

        assertEquals(123, taskId);
        assertTrue(repo.created);
        DomainEvent event = publisher.lastEvent.get();
        assertNotNull(event);
        assertInstanceOf(EvaluationTaskPostedEvent.class, event);
        assertEquals(123, ((EvaluationTaskPostedEvent) event).taskId());
        assertEquals(20, ((EvaluationTaskPostedEvent) event).evaluatorId());
    }

    private static class FakeRepo implements PostEvaTaskRepository {
        private final Integer taskId;
        private boolean created;

        private FakeRepo(Integer taskId) {
            this.taskId = taskId;
        }

        @Override
        public Integer create(PostEvaTaskCommand command, Integer maxBeEvaNum) {
            this.created = true;
            return taskId;
        }
    }

    private static class FakePublisher implements DomainEventPublisher {
        private final AtomicReference<DomainEvent> lastEvent = new AtomicReference<>();

        @Override
        public void publishAfterCommit(DomainEvent event) {
            lastEvent.set(event);
        }
    }
}

