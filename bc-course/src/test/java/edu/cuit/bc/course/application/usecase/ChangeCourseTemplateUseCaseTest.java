package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.ChangeCourseTemplateCommand;
import edu.cuit.bc.course.application.port.ChangeCourseTemplateRepository;
import edu.cuit.bc.course.domain.ChangeCourseTemplateException;
import edu.cuit.bc.template.application.CourseTemplateLockService;
import edu.cuit.bc.template.application.port.CourseTemplateLockQueryPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChangeCourseTemplateUseCaseTest {

    @Test
    void execute_whenAnyCourseLocked_shouldFailAndNotPersist() {
        RecordingRepository repository = new RecordingRepository();
        CourseTemplateLockService lockService = new CourseTemplateLockService(new CourseTemplateLockQueryPort() {
            @Override
            public boolean isLocked(Integer courseId, Integer semesterId) {
                return courseId != null && courseId == 2;
            }
        });
        ChangeCourseTemplateUseCase useCase = new ChangeCourseTemplateUseCase(repository, lockService);

        ChangeCourseTemplateCommand cmd = new ChangeCourseTemplateCommand(1, 100, List.of(1, 2, 3));
        ChangeCourseTemplateException ex = assertThrows(ChangeCourseTemplateException.class, () -> useCase.execute(cmd));

        assertTrue(ex.getMessage().contains("课程ID"));
        assertEquals(List.of(2), ex.getLockedCourseIds());
        assertEquals(0, repository.invocations.size());
    }

    @Test
    void execute_whenAllUnlocked_shouldPersistOnce() {
        RecordingRepository repository = new RecordingRepository();
        CourseTemplateLockService lockService = new CourseTemplateLockService((courseId, semesterId) -> false);
        ChangeCourseTemplateUseCase useCase = new ChangeCourseTemplateUseCase(repository, lockService);

        ChangeCourseTemplateCommand cmd = new ChangeCourseTemplateCommand(1, 100, List.of(1, 2, 3));
        assertDoesNotThrow(() -> useCase.execute(cmd));

        assertEquals(1, repository.invocations.size());
        assertEquals(1, repository.invocations.get(0).semesterId);
        assertEquals(100, repository.invocations.get(0).templateId);
        assertEquals(List.of(1, 2, 3), repository.invocations.get(0).courseIds);
    }

    private static class RecordingRepository implements ChangeCourseTemplateRepository {
        private final List<Invocation> invocations = new ArrayList<>();

        @Override
        public void changeTemplate(Integer semesterId, Integer templateId, List<Integer> courseIdList) {
            invocations.add(new Invocation(semesterId, templateId, List.copyOf(courseIdList)));
        }

        private record Invocation(Integer semesterId, Integer templateId, List<Integer> courseIds) { }
    }
}

