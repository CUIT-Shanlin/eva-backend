package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.UpdateCoursesTypeCommand;
import edu.cuit.bc.course.application.port.UpdateCoursesTypeRepository;
import edu.cuit.client.dto.cmd.course.UpdateCoursesToTypeCmd;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateCoursesTypeUseCaseTest {

    @Test
    void execute_whenNullCommand_shouldThrow() {
        UpdateCoursesTypeUseCase useCase = new UpdateCoursesTypeUseCase(new NoopRepository());
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_shouldDelegateToRepository() {
        RecordingRepository repository = new RecordingRepository();
        UpdateCoursesTypeUseCase useCase = new UpdateCoursesTypeUseCase(repository);

        UpdateCoursesToTypeCmd dto = new UpdateCoursesToTypeCmd();
        dto.setCourseIdList(List.of(10, 20));
        dto.setTypeIdList(List.of(1, 2));

        useCase.execute(new UpdateCoursesTypeCommand(dto));

        assertEquals(1, repository.calls);
        assertEquals(List.of(10, 20), repository.courseIds);
        assertEquals(List.of(1, 2), repository.typeIds);
    }

    private static class NoopRepository implements UpdateCoursesTypeRepository {
        @Override
        public void update(UpdateCoursesTypeCommand command) {
        }
    }

    private static class RecordingRepository implements UpdateCoursesTypeRepository {
        private int calls = 0;
        private List<Integer> courseIds;
        private List<Integer> typeIds;

        @Override
        public void update(UpdateCoursesTypeCommand command) {
            this.calls++;
            this.courseIds = command.updateCoursesToTypeCmd().getCourseIdList();
            this.typeIds = command.updateCoursesToTypeCmd().getTypeIdList();
        }
    }
}
