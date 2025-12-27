package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.UpdateCourseInfoCommand;
import edu.cuit.bc.course.application.port.UpdateCourseInfoRepository;
import edu.cuit.bc.course.domain.UpdateCourseInfoException;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateCourseInfoUseCaseTest {

    @Test
    void execute_whenMissingFields_shouldThrow() {
        UpdateCourseInfoUseCase useCase = new UpdateCourseInfoUseCase(new NoopRepository());

        assertThrows(UpdateCourseInfoException.class, () -> useCase.execute(new UpdateCourseInfoCommand(null, new UpdateCourseCmd().setId(1))));
        assertThrows(UpdateCourseInfoException.class, () -> useCase.execute(new UpdateCourseInfoCommand(1, null)));
        assertThrows(UpdateCourseInfoException.class, () -> useCase.execute(new UpdateCourseInfoCommand(1, new UpdateCourseCmd())));
    }

    @Test
    void execute_shouldDelegateToRepository() {
        RecordingRepository repository = new RecordingRepository();
        UpdateCourseInfoUseCase useCase = new UpdateCourseInfoUseCase(repository);

        UpdateCourseCmd cmd = new UpdateCourseCmd().setId(10);
        Map<String, Map<Integer, Integer>> result = useCase.execute(new UpdateCourseInfoCommand(1, cmd));

        assertEquals(1, repository.calls);
        assertEquals(1, repository.semesterId);
        assertEquals(10, repository.courseId);
        assertEquals(result, repository.returnValue);
    }

    private static class NoopRepository implements UpdateCourseInfoRepository {
        @Override
        public Map<String, Map<Integer, Integer>> update(UpdateCourseInfoCommand command) {
            return Map.of();
        }
    }

    private static class RecordingRepository implements UpdateCourseInfoRepository {
        private int calls = 0;
        private Integer semesterId;
        private Integer courseId;
        private final Map<String, Map<Integer, Integer>> returnValue = new HashMap<>();

        @Override
        public Map<String, Map<Integer, Integer>> update(UpdateCourseInfoCommand command) {
            this.calls++;
            this.semesterId = command.semesterId();
            this.courseId = command.updateCourseCmd().getId();
            return returnValue;
        }
    }
}

