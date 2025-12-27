package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.AssignEvaTeachersCommand;
import edu.cuit.bc.course.application.port.AssignEvaTeachersRepository;
import edu.cuit.bc.course.domain.AssignEvaTeachersException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssignEvaTeachersUseCaseTest {

    @Test
    void execute_whenMissingIds_shouldThrow() {
        AssignEvaTeachersUseCase useCase = new AssignEvaTeachersUseCase(new NoopRepository());

        assertThrows(AssignEvaTeachersException.class, () -> useCase.execute(new AssignEvaTeachersCommand(null, 1, List.of(1))));
        assertThrows(AssignEvaTeachersException.class, () -> useCase.execute(new AssignEvaTeachersCommand(1, null, List.of(1))));
        assertThrows(AssignEvaTeachersException.class, () -> useCase.execute(new AssignEvaTeachersCommand(1, 1, List.of())));
    }

    @Test
    void execute_shouldDelegateToRepository() {
        RecordingRepository repository = new RecordingRepository();
        AssignEvaTeachersUseCase useCase = new AssignEvaTeachersUseCase(repository);

        Map<String, Map<Integer, Integer>> result = useCase.execute(new AssignEvaTeachersCommand(1, 10, List.of(2, 3)));

        assertEquals(1, repository.calls);
        assertEquals(1, repository.semesterId);
        assertEquals(10, repository.courInfId);
        assertEquals(List.of(2, 3), repository.teacherIds);
        assertEquals(result, repository.returnValue);
    }

    private static class NoopRepository implements AssignEvaTeachersRepository {
        @Override
        public Map<String, Map<Integer, Integer>> assign(Integer semesterId, Integer courInfId, List<Integer> evaTeacherIdList) {
            return Map.of();
        }
    }

    private static class RecordingRepository implements AssignEvaTeachersRepository {
        private int calls = 0;
        private Integer semesterId;
        private Integer courInfId;
        private List<Integer> teacherIds;
        private final Map<String, Map<Integer, Integer>> returnValue = new HashMap<>();

        @Override
        public Map<String, Map<Integer, Integer>> assign(Integer semesterId, Integer courInfId, List<Integer> evaTeacherIdList) {
            this.calls++;
            this.semesterId = semesterId;
            this.courInfId = courInfId;
            this.teacherIds = List.copyOf(evaTeacherIdList);
            return returnValue;
        }
    }
}

