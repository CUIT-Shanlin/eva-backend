package edu.cuit.app.ai;

import edu.cuit.app.poi.ai.AiReportExporter;
import edu.cuit.client.bo.ai.AiAnalysisBO;
import edu.cuit.client.bo.ai.AiCourseSuggestionBO;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;

public class CourseAnalysisTest {

    @Test
    public void testExportFile() throws IOException {
        System.setProperty("java.awt.headless", "true");
        AiAnalysisBO analysis = new AiAnalysisBO()
                .setCourseSuggestions(List.of(new AiCourseSuggestionBO()
                        .setCourseName("Web应用开发A")
                        .setSuggestion("1. 重新审视并调整教学目标和计划，确保它们更加合理且符合学生的学习需求。\n2. 增加更多高质量的教学资源，并确保这些材料易于访问。")
                        .setBeEvaNumCount(5)
                        .setHighScoreBeEvaCount(0)
                        .setAdventures("1. 课堂互动积极，能够激发学生参与热情。")
                        .setDrawbacks("1. 课程资源的丰富性和可用性得分不高。")
                        )
                )
                .setTotalBeEvaCount(5)
                .setHighScoreEvaCount(0)
                .setOverallReport("综合分析该教师的教学情况，教学方法表现较好，但课程资源仍需完善。")
                .setTeacherName("张三");
        try (XWPFDocument document = new AiReportExporter().generateReport(analysis);
             ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            document.write(stream);
            org.junit.jupiter.api.Assertions.assertTrue(stream.size() > 0);
        }
    }

}
