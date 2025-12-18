package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.AddNotExistCoursesDetailsCommand;
import edu.cuit.bc.course.application.port.AddNotExistCoursesDetailsRepository;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AddNotExistCoursesDetailsUseCaseTest {

    @Test
    void execute_whenNullCommand_shouldThrow() {
        AddNotExistCoursesDetailsUseCase useCase = new AddNotExistCoursesDetailsUseCase(new NoopRepository());
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_shouldDelegateToRepository() {
        RecordingRepository repository = new RecordingRepository();
        AddNotExistCoursesDetailsUseCase useCase = new AddNotExistCoursesDetailsUseCase(repository);

        Integer semId = 1;
        Integer teacherId = 10;
        UpdateCourseCmd courseInfo = new UpdateCourseCmd();
        List<SelfTeachCourseTimeCO> dateArr = List.of();

        useCase.execute(new AddNotExistCoursesDetailsCommand(semId, teacherId, courseInfo, dateArr));

        assertEquals(1, repository.calls);
        assertEquals(semId, repository.semesterId);
        assertEquals(teacherId, repository.teacherId);
        assertEquals(courseInfo, repository.courseInfo);
        assertEquals(dateArr, repository.dateArr);
    }

    private static class NoopRepository implements AddNotExistCoursesDetailsRepository {
        @Override
        public void add(Integer semesterId, Integer teacherId, UpdateCourseCmd courseInfo, List<SelfTeachCourseTimeCO> dateArr) {
        }
    }

    private static class RecordingRepository implements AddNotExistCoursesDetailsRepository {
        private int calls = 0;
        private Integer semesterId;
        private Integer teacherId;
        private UpdateCourseCmd courseInfo;
        private List<SelfTeachCourseTimeCO> dateArr;

        @Override
        public void add(Integer semesterId, Integer teacherId, UpdateCourseCmd courseInfo, List<SelfTeachCourseTimeCO> dateArr) {
            this.calls++;
            this.semesterId = semesterId;
            this.teacherId = teacherId;
            this.courseInfo = courseInfo;
            this.dateArr = dateArr;
        }
    }
}

