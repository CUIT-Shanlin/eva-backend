package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.DeleteCourseTypeCommand;
import edu.cuit.bc.course.application.port.DeleteCourseTypeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeleteCourseTypeUseCaseTest {

    @Test
    void execute_whenNullCommand_shouldThrow() {
        DeleteCourseTypeUseCase useCase = new DeleteCourseTypeUseCase(new NoopRepository());
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_shouldDelegateToRepository() {
        RecordingRepository repository = new RecordingRepository();
        DeleteCourseTypeUseCase useCase = new DeleteCourseTypeUseCase(repository);

        List<Integer> typeIds = List.of(1, 2, 3);
        useCase.execute(new DeleteCourseTypeCommand(typeIds));

        assertEquals(1, repository.calls);
        assertEquals(typeIds, repository.typeIds);
    }

    private static class NoopRepository implements DeleteCourseTypeRepository {
        @Override
        public void delete(List<Integer> typeIds) {
        }
    }

    private static class RecordingRepository implements DeleteCourseTypeRepository {
        private int calls = 0;
        private List<Integer> typeIds;

        @Override
        public void delete(List<Integer> typeIds) {
            this.calls++;
            this.typeIds = typeIds;
        }
    }
}

