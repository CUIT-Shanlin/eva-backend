package edu.cuit.bc.template.application;

import edu.cuit.bc.template.application.port.CourseTemplateLockQueryPort;
import edu.cuit.bc.template.domain.TemplateLockedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CourseTemplateLockServiceTest {

    @Test
    void assertCanChangeTemplate_whenLocked_shouldThrow() {
        CourseTemplateLockQueryPort port = (courseId, semesterId) -> true;
        CourseTemplateLockService service = new CourseTemplateLockService(port);

        assertThrows(TemplateLockedException.class, () -> service.assertCanChangeTemplate(1, 1));
    }

    @Test
    void assertCanChangeTemplate_whenNotLocked_shouldPass() {
        CourseTemplateLockQueryPort port = (courseId, semesterId) -> false;
        CourseTemplateLockService service = new CourseTemplateLockService(port);

        assertDoesNotThrow(() -> service.assertCanChangeTemplate(1, 1));
    }
}

