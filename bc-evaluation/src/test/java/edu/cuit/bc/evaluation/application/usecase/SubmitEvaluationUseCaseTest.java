package edu.cuit.bc.evaluation.application.usecase;

import edu.cuit.bc.evaluation.application.model.FormPropValue;
import edu.cuit.bc.evaluation.application.model.SubmitEvaluationCommand;
import edu.cuit.bc.evaluation.application.model.SubmitEvaluationContext;
import edu.cuit.bc.evaluation.application.port.DomainEventPublisher;
import edu.cuit.bc.evaluation.application.port.SubmitEvaluationRepository;
import edu.cuit.bc.evaluation.domain.SubmitEvaluationException;
import edu.cuit.bc.evaluation.domain.TaskStatus;
import edu.cuit.bc.evaluation.domain.event.DomainEvent;
import edu.cuit.bc.evaluation.domain.event.EvaluationSubmittedEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SubmitEvaluationUseCaseTest {

    @Test
    void submit_whenTaskMissing_shouldThrow() {
        SubmitEvaluationRepository repo = new FakeRepo(null);
        FakePublisher publisher = new FakePublisher();
        SubmitEvaluationUseCase useCase = new SubmitEvaluationUseCase(repo, publisher);

        SubmitEvaluationException ex = assertThrows(SubmitEvaluationException.class,
                () -> useCase.submit(new SubmitEvaluationCommand(1, "ok", List.of())));
        assertEquals("该任务不存在", ex.getMessage());
        assertNull(publisher.lastEvent.get());
    }

    @Test
    void submit_whenTaskNotPending_shouldThrow() {
        SubmitEvaluationContext ctx = new SubmitEvaluationContext(
                1, 2, "张三", 10, 20, 30, 40, "模板", "desc", "props", TaskStatus.COMPLETED
        );
        SubmitEvaluationRepository repo = new FakeRepo(ctx);
        FakePublisher publisher = new FakePublisher();
        SubmitEvaluationUseCase useCase = new SubmitEvaluationUseCase(repo, publisher);

        SubmitEvaluationException ex = assertThrows(SubmitEvaluationException.class,
                () -> useCase.submit(new SubmitEvaluationCommand(1, "ok", List.of())));
        assertEquals("该任务已完成或已撤回，不能提交", ex.getMessage());
        assertNull(publisher.lastEvent.get());
    }

    @Test
    void submit_whenOk_shouldSaveAndPublish() {
        SubmitEvaluationContext ctx = new SubmitEvaluationContext(
                1, 2, "张三", 10, 20, 30, 40, "模板", "desc", "props", TaskStatus.PENDING
        );
        FakeRepo repo = new FakeRepo(ctx);
        FakePublisher publisher = new FakePublisher();
        SubmitEvaluationUseCase useCase = new SubmitEvaluationUseCase(repo, publisher);

        useCase.submit(new SubmitEvaluationCommand(1, "很不错", List.of(new FormPropValue("指标1", 90))));

        assertTrue(repo.saved);
        DomainEvent event = publisher.lastEvent.get();
        assertNotNull(event);
        assertInstanceOf(EvaluationSubmittedEvent.class, event);
        assertEquals(1, ((EvaluationSubmittedEvent) event).taskId());
        assertEquals(2, ((EvaluationSubmittedEvent) event).evaluatorId());
        assertEquals(20, ((EvaluationSubmittedEvent) event).courseId());
        assertEquals(30, ((EvaluationSubmittedEvent) event).semesterId());
    }

    private static class FakeRepo implements SubmitEvaluationRepository {
        private final SubmitEvaluationContext context;
        private boolean saved;

        private FakeRepo(SubmitEvaluationContext context) {
            this.context = context;
        }

        @Override
        public SubmitEvaluationContext loadContext(Integer taskId) {
            return context;
        }

        @Override
        public void saveEvaluation(SubmitEvaluationContext context, String textValue, List<FormPropValue> formPropsValues) {
            this.saved = true;
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

