package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.AddExistCoursesDetailsCommand;
import edu.cuit.bc.course.application.port.AddExistCoursesDetailsRepository;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AddExistCoursesDetailsUseCaseTest {

    @Test
    void execute_whenNullCommand_shouldThrow() {
        AddExistCoursesDetailsUseCase useCase = new AddExistCoursesDetailsUseCase(new NoopRepository());
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_shouldDelegateToRepository() {
        RecordingRepository repository = new RecordingRepository();
        AddExistCoursesDetailsUseCase useCase = new AddExistCoursesDetailsUseCase(repository);

        Integer courseId = 100;
        SelfTeachCourseTimeCO timeCO = new SelfTeachCourseTimeCO();

        useCase.execute(new AddExistCoursesDetailsCommand(courseId, timeCO));

        assertEquals(1, repository.calls);
        assertEquals(courseId, repository.courseId);
        assertEquals(timeCO, repository.timeCO);
    }

    private static class NoopRepository implements AddExistCoursesDetailsRepository {
        @Override
        public void add(Integer courseId, SelfTeachCourseTimeCO timeCO) {
        }
    }

    private static class RecordingRepository implements AddExistCoursesDetailsRepository {
        private int calls = 0;
        private Integer courseId;
        private SelfTeachCourseTimeCO timeCO;

        @Override
        public void add(Integer courseId, SelfTeachCourseTimeCO timeCO) {
            this.calls++;
            this.courseId = courseId;
            this.timeCO = timeCO;
        }
    }
}

