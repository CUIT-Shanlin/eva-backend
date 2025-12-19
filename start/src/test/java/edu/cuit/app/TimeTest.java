package edu.cuit.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.cuit.client.dto.clientobject.SemesterCO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

public class TimeTest {

    @Test
    public void testTime() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        SemesterCO semesterCO = objectMapper.readValue("{    \"period\": 1,    \"startYear\": \"2023\",    \"endYear\": \"2024\",    \"startDate\": \"2024-09-02\"}", SemesterCO.class);
        LocalDate startDate = semesterCO.getStartDate();
        org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(2024, 9, 2), startDate);
    }

}
