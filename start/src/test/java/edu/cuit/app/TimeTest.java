package edu.cuit.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cuit.client.dto.clientobject.SemesterCO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

@SpringBootTest
public class TimeTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testTime() throws JsonProcessingException {
        SemesterCO semesterCO = objectMapper.readValue("{    \"period\": 1,    \"startYear\": \"2023\",    \"endYear\": \"2024\",    \"startDate\": \"2024-09-02\"}", SemesterCO.class);
        System.out.println(semesterCO.getStartDate().toString());
    }

}
