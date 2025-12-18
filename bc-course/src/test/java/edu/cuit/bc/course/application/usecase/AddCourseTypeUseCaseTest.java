package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.AddCourseTypeCommand;
import edu.cuit.bc.course.application.port.AddCourseTypeRepository;
import edu.cuit.client.dto.data.course.CourseType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AddCourseTypeUseCaseTest {

    @Test
    void execute_whenNullCommand_shouldThrow() {
        AddCourseTypeUseCase useCase = new AddCourseTypeUseCase(new NoopRepository());
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_shouldDelegateToRepository() {
        RecordingRepository repository = new RecordingRepository();
        AddCourseTypeUseCase useCase = new AddCourseTypeUseCase(repository);

        CourseType courseType = new CourseType();
        courseType.setName("理论课");
        courseType.setDescription("desc");
        useCase.execute(new AddCourseTypeCommand(courseType));

        assertEquals(1, repository.calls);
        assertEquals(courseType, repository.courseType);
    }

    private static class NoopRepository implements AddCourseTypeRepository {
        @Override
        public void add(CourseType courseType) {
        }
    }

    private static class RecordingRepository implements AddCourseTypeRepository {
        private int calls = 0;
        private CourseType courseType;

        @Override
        public void add(CourseType courseType) {
            this.calls++;
            this.courseType = courseType;
        }
    }
}

