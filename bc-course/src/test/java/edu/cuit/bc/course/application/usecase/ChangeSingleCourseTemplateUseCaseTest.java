package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.ChangeSingleCourseTemplateCommand;
import edu.cuit.bc.course.application.port.ChangeCourseTemplateRepository;
import edu.cuit.bc.course.application.port.CourseTemplateIdQueryPort;
import edu.cuit.bc.course.domain.ChangeCourseTemplateException;
import edu.cuit.bc.course.domain.CourseNotFoundException;
import edu.cuit.bc.template.application.CourseTemplateLockService;
import edu.cuit.bc.template.application.port.CourseTemplateLockQueryPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ChangeSingleCourseTemplateUseCaseTest {

    @Test
    void execute_whenTemplateIdNull_shouldDoNothing() {
        RecordingRepository repository = new RecordingRepository();
        ChangeSingleCourseTemplateUseCase useCase = new ChangeSingleCourseTemplateUseCase(
                (semesterId, courseId) -> Optional.of(1),
                repository,
                new CourseTemplateLockService((courseId, semesterId) -> false)
        );

        assertDoesNotThrow(() -> useCase.execute(new ChangeSingleCourseTemplateCommand(1, 10, null)));
        assertEquals(0, repository.invocations.size());
    }

    @Test
    void execute_whenTemplateUnchanged_shouldNotCheckLockOrPersist() {
        RecordingRepository repository = new RecordingRepository();
        CourseTemplateIdQueryPort templateIdQueryPort = (semesterId, courseId) -> Optional.of(100);
        CourseTemplateLockService lockService = new CourseTemplateLockService(new CourseTemplateLockQueryPort() {
            @Override
            public boolean isLocked(Integer courseId, Integer semesterId) {
                throw new AssertionError("模板未变化时不应触发锁定校验");
            }
        });
        ChangeSingleCourseTemplateUseCase useCase = new ChangeSingleCourseTemplateUseCase(
                templateIdQueryPort,
                repository,
                lockService
        );

        assertDoesNotThrow(() -> useCase.execute(new ChangeSingleCourseTemplateCommand(1, 10, 100)));
        assertEquals(0, repository.invocations.size());
    }

    @Test
    void execute_whenCourseNotFound_shouldThrow() {
        RecordingRepository repository = new RecordingRepository();
        ChangeSingleCourseTemplateUseCase useCase = new ChangeSingleCourseTemplateUseCase(
                (semesterId, courseId) -> Optional.empty(),
                repository,
                new CourseTemplateLockService((courseId, semesterId) -> false)
        );

        assertThrows(CourseNotFoundException.class, () -> useCase.execute(new ChangeSingleCourseTemplateCommand(1, 10, 200)));
        assertEquals(0, repository.invocations.size());
    }

    @Test
    void execute_whenTemplateChangedButLocked_shouldThrowAndNotPersist() {
        RecordingRepository repository = new RecordingRepository();
        ChangeSingleCourseTemplateUseCase useCase = new ChangeSingleCourseTemplateUseCase(
                (semesterId, courseId) -> Optional.of(100),
                repository,
                new CourseTemplateLockService((courseId, semesterId) -> true)
        );

        ChangeCourseTemplateException ex = assertThrows(
                ChangeCourseTemplateException.class,
                () -> useCase.execute(new ChangeSingleCourseTemplateCommand(1, 10, 200))
        );
        assertEquals(List.of(10), ex.getLockedCourseIds());
        assertEquals(0, repository.invocations.size());
    }

    @Test
    void execute_whenTemplateChangedAndUnlocked_shouldPersistOnce() {
        RecordingRepository repository = new RecordingRepository();
        ChangeSingleCourseTemplateUseCase useCase = new ChangeSingleCourseTemplateUseCase(
                (semesterId, courseId) -> Optional.of(100),
                repository,
                new CourseTemplateLockService((courseId, semesterId) -> false)
        );

        assertDoesNotThrow(() -> useCase.execute(new ChangeSingleCourseTemplateCommand(1, 10, 200)));
        assertEquals(1, repository.invocations.size());
        assertEquals(1, repository.invocations.get(0).semesterId);
        assertEquals(200, repository.invocations.get(0).templateId);
        assertEquals(List.of(10), repository.invocations.get(0).courseIds);
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

