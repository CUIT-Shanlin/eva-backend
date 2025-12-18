package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.DeleteSelfCourseCommand;
import edu.cuit.bc.course.application.port.DeleteSelfCourseRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeleteSelfCourseUseCaseTest {

    @Test
    void execute_whenNullCommand_shouldThrow() {
        DeleteSelfCourseUseCase useCase = new DeleteSelfCourseUseCase(new NoopRepository());
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_shouldDelegateToRepository() {
        RecordingRepository repository = new RecordingRepository();
        DeleteSelfCourseUseCase useCase = new DeleteSelfCourseUseCase(repository);

        Map<String, Map<Integer, Integer>> result = useCase.execute(new DeleteSelfCourseCommand("u", 10));

        assertEquals(1, repository.calls);
        assertEquals("u", repository.username);
        assertEquals(10, repository.courseId);
        assertEquals(result, repository.returnValue);
    }

    private static class NoopRepository implements DeleteSelfCourseRepository {
        @Override
        public Map<String, Map<Integer, Integer>> delete(String username, Integer courseId) {
            return Map.of();
        }
    }

    private static class RecordingRepository implements DeleteSelfCourseRepository {
        private int calls = 0;
        private String username;
        private Integer courseId;
        private final Map<String, Map<Integer, Integer>> returnValue = new HashMap<>();

        @Override
        public Map<String, Map<Integer, Integer>> delete(String username, Integer courseId) {
            this.calls++;
            this.username = username;
            this.courseId = courseId;
            return returnValue;
        }
    }
}

