package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.DeleteCourseCommand;
import edu.cuit.bc.course.application.port.DeleteCourseRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeleteCourseUseCaseTest {

    @Test
    void execute_whenNullCommand_shouldThrow() {
        DeleteCourseUseCase useCase = new DeleteCourseUseCase(new NoopRepository());
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_shouldDelegateToRepository() {
        RecordingRepository repository = new RecordingRepository();
        DeleteCourseUseCase useCase = new DeleteCourseUseCase(repository);

        Map<String, Map<Integer, Integer>> result = useCase.execute(new DeleteCourseCommand(1, 10));

        assertEquals(1, repository.calls);
        assertEquals(1, repository.semesterId);
        assertEquals(10, repository.courseId);
        assertEquals(result, repository.returnValue);
    }

    private static class NoopRepository implements DeleteCourseRepository {
        @Override
        public Map<String, Map<Integer, Integer>> delete(Integer semesterId, Integer courseId) {
            return Map.of();
        }
    }

    private static class RecordingRepository implements DeleteCourseRepository {
        private int calls = 0;
        private Integer semesterId;
        private Integer courseId;
        private final Map<String, Map<Integer, Integer>> returnValue = new HashMap<>();

        @Override
        public Map<String, Map<Integer, Integer>> delete(Integer semesterId, Integer courseId) {
            this.calls++;
            this.semesterId = semesterId;
            this.courseId = courseId;
            return returnValue;
        }
    }
}

