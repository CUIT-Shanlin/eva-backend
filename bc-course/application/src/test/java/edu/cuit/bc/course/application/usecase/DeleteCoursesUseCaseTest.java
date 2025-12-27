package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.DeleteCoursesCommand;
import edu.cuit.bc.course.application.port.DeleteCoursesRepository;
import edu.cuit.client.dto.data.course.CoursePeriod;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeleteCoursesUseCaseTest {

    @Test
    void execute_whenNullCommand_shouldThrow() {
        DeleteCoursesUseCase useCase = new DeleteCoursesUseCase(new NoopRepository());
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_shouldDelegateToRepository() {
        RecordingRepository repository = new RecordingRepository();
        DeleteCoursesUseCase useCase = new DeleteCoursesUseCase(repository);

        CoursePeriod period = new CoursePeriod();
        Map<String, Map<Integer, Integer>> result = useCase.execute(new DeleteCoursesCommand(1, 100, period));

        assertEquals(1, repository.calls);
        assertEquals(1, repository.semesterId);
        assertEquals(100, repository.courInfId);
        assertEquals(result, repository.returnValue);
    }

    private static class NoopRepository implements DeleteCoursesRepository {
        @Override
        public Map<String, Map<Integer, Integer>> delete(Integer semesterId, Integer courInfId, CoursePeriod coursePeriod) {
            return Map.of();
        }
    }

    private static class RecordingRepository implements DeleteCoursesRepository {
        private int calls = 0;
        private Integer semesterId;
        private Integer courInfId;
        private final Map<String, Map<Integer, Integer>> returnValue = new HashMap<>();

        @Override
        public Map<String, Map<Integer, Integer>> delete(Integer semesterId, Integer courInfId, CoursePeriod coursePeriod) {
            this.calls++;
            this.semesterId = semesterId;
            this.courInfId = courInfId;
            return returnValue;
        }
    }
}
