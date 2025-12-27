package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.port.CourseImportedQueryPort;
import edu.cuit.client.dto.data.Term;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IsCourseImportedUseCaseTest {

    @Test
    void execute_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        IsCourseImportedUseCase useCase = new IsCourseImportedUseCase(port);

        Term term = new Term();
        Boolean result = useCase.execute(1, term);

        assertEquals(1, port.calls);
        assertEquals(1, port.type);
        assertEquals(term, port.term);
        assertEquals(Boolean.TRUE, result);
    }

    private static class RecordingPort implements CourseImportedQueryPort {
        private int calls = 0;
        private Integer type;
        private Term term;

        @Override
        public Boolean isImported(Integer type, Term term) {
            this.calls++;
            this.type = type;
            this.term = term;
            return Boolean.TRUE;
        }
    }
}

