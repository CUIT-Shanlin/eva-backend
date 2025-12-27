package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.UpdateCourseTypeCommand;
import edu.cuit.bc.course.application.port.UpdateCourseTypeRepository;
import edu.cuit.client.dto.cmd.course.UpdateCourseTypeCmd;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateCourseTypeUseCaseTest {

    @Test
    void execute_whenNullCommand_shouldThrow() {
        UpdateCourseTypeUseCase useCase = new UpdateCourseTypeUseCase(new NoopRepository());
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_shouldDelegateToRepository() {
        RecordingRepository repository = new RecordingRepository();
        UpdateCourseTypeUseCase useCase = new UpdateCourseTypeUseCase(repository);

        UpdateCourseTypeCmd dto = new UpdateCourseTypeCmd();
        dto.setId(1);
        useCase.execute(new UpdateCourseTypeCommand(dto));

        assertEquals(1, repository.calls);
        assertEquals(1, repository.typeId);
    }

    private static class NoopRepository implements UpdateCourseTypeRepository {
        @Override
        public void update(UpdateCourseTypeCommand command) {
        }
    }

    private static class RecordingRepository implements UpdateCourseTypeRepository {
        private int calls = 0;
        private Integer typeId;

        @Override
        public void update(UpdateCourseTypeCommand command) {
            this.calls++;
            this.typeId = command.updateCourseTypeCmd().getId();
        }
    }
}

