package edu.cuit.app.ai;

import edu.cuit.client.api.ai.IAiCourseAnalysisService;
import edu.cuit.client.bo.ai.AiAnalysisBO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CourseAnalysisTest {

    @Autowired
    private IAiCourseAnalysisService aiCourseAnalysisService;

    @Test
    public void testSuggestion() {
        AiAnalysisBO analysis = aiCourseAnalysisService.analysis(2, 6);
        System.out.println(analysis);
    }

}
