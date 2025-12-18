package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.UpdateSelfCourseCommand;
import edu.cuit.bc.course.application.port.UpdateSelfCourseRepository;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeInfoCO;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateSelfCourseUseCaseTest {

    @Test
    void execute_whenNullCommand_shouldThrow() {
        UpdateSelfCourseUseCase useCase = new UpdateSelfCourseUseCase(new NoopRepository());
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_shouldDelegateToRepository() {
        RecordingRepository repository = new RecordingRepository();
        UpdateSelfCourseUseCase useCase = new UpdateSelfCourseUseCase(repository);

        SelfTeachCourseCO course = new SelfTeachCourseCO();
        course.setId(10);
        List<SelfTeachCourseTimeInfoCO> timeList = List.of(new SelfTeachCourseTimeInfoCO());

        Map<String, Map<Integer, Integer>> result = useCase.execute(new UpdateSelfCourseCommand("u", course, timeList));

        assertEquals(1, repository.calls);
        assertEquals("u", repository.username);
        assertEquals(10, repository.courseId);
        assertEquals(1, repository.timeListSize);
        assertEquals(result, repository.returnValue);
    }

    private static class NoopRepository implements UpdateSelfCourseRepository {
        @Override
        public Map<String, Map<Integer, Integer>> update(String username, SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeInfoCO> timeList) {
            return Map.of();
        }
    }

    private static class RecordingRepository implements UpdateSelfCourseRepository {
        private int calls = 0;
        private String username;
        private Integer courseId;
        private int timeListSize;
        private final Map<String, Map<Integer, Integer>> returnValue = new HashMap<>();

        @Override
        public Map<String, Map<Integer, Integer>> update(String username, SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeInfoCO> timeList) {
            this.calls++;
            this.username = username;
            this.courseId = selfTeachCourseCO.getId();
            this.timeListSize = timeList == null ? -1 : timeList.size();
            return returnValue;
        }
    }
}

