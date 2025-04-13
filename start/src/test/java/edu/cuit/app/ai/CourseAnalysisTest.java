package edu.cuit.app.ai;

import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelFileUtil;
import cn.hutool.poi.excel.ExcelUtil;
import edu.cuit.app.poi.ai.AiReportExporter;
import edu.cuit.client.api.ai.IAiCourseAnalysisService;
import edu.cuit.client.bo.ai.AiAnalysisBO;
import edu.cuit.client.bo.ai.AiCourseSuggestionBO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.util.List;

@SpringBootTest
public class CourseAnalysisTest {

    @Autowired
    private IAiCourseAnalysisService aiCourseAnalysisService;

    @Test
    public void testSuggestion() {
        AiAnalysisBO analysis = aiCourseAnalysisService.analysis(2, 6);
        System.out.println(JSONUtil.parse(analysis).toStringPretty());
        System.out.println("======================================================");
        System.out.println(analysis);
    }

    @Test
//    @org.junit.Test
    public void testExportFile() throws IOException {
        AiAnalysisBO analysis = aiCourseAnalysisService.analysis(2, 7);
        System.out.println(JSONUtil.parse(analysis).toStringPretty());
        System.out.println("======================================================");
        System.out.println(analysis);
        /*AiAnalysisBO analysis = new AiAnalysisBO()
                .setCourseSuggestions(List.of(new AiCourseSuggestionBO()
                        .setCourseName("Web应用开发A")
                        .setSuggestion("1. 重新审视并调整教学目标和计划，确保它们更加合理且符合学生的学习需求。可以考虑增加与学生讨论课程内容的机会，以更好地了解他们的兴趣点和难点。\\n2. 增加更多高质量的教学资源，比如视频、文章链接等，并确保这些材料易于访问。同时也可以鼓励学生分享他们发现的有用资源。\\n3. 加强课堂管理技巧，比如明确课堂规则、使用积极的语言来引导学生行为等，创造一个更有序的学习环境。\\n4. 提前做好充分准备，包括熟悉讲授的内容以及准备好所有需要用到的教学工具或设备，避免因技术问题而打断上课节奏。\\n5. 调整每节课的内容量，确保能够充分利用好每一分钟，如果经常提前完成，则可以考虑加入更多互动环节或者深入探讨某些话题，使课程更加充实。")
                        .setBeEvaNumCount(5)
                        .setHighScoreBeEvaCount(0)
                        .setAdventures("1. 该老师擅长使用通俗易懂的例子来解释复杂概念，这种教学方法能够有效提升学生对知识的理解和兴趣。\\n2. 在授课过程中，老师善于将理论与实践相结合，通过联系实际案例以及采用图文并茂的方式进行讲解，使得课程内容更加生动有趣。\\n3. 老师能够利用现代教育技术手段如多媒体课件等辅助工具来进行教学，这不仅使课堂氛围更加活跃，也促进了师生之间的互动交流。\\n4. 为了鼓励学生积极参与课堂讨论，老师会经常性地向学生提问，以此激发学生的思考能力和参与热情。\\n5. 从部分评价来看，张老师在准备充分的情况下能够展现出较强的课堂组织管理能力，并且能够有效地维持良好的课堂秩序。")
                        .setDrawbacks("1. 教学目标和教学计划的合理性评分普遍较低，这表明老师在课程设计和时间安排上可能存在不足。\\n2. 课程资源的丰富性和可用性得分不高，说明提供的学习材料可能不够全面或难以获取。\\n3. 有评价指出课堂纪律有待提高，这可能意味着老师在课堂管理方面存在一些问题。\\n4. 部分评价提到老师有时准备不充分，这可能会影响教学质量。\\n5. 有反馈提到提前结束课程，学生可能意犹未尽，这表明课程内容安排上可能存在不够紧凑的问题。")
                        )
                )
                .setTotalBeEvaCount(5)
                .setHighScoreEvaCount(0)
                .setOverallReport("综合分析该教师的教学情况，可以看出其在教学方法和课堂互动方面表现较为突出，能够通过多种方式激发学生的学习兴趣，并有效提升课堂参与度。然而，在教学计划执行、课堂纪律管理以及备课准备方面存在一定不足，影响了整体教学效果。\\n\\n建议该教师在以下几个方面进行改进：\\n1. 严格遵守课时安排，确保每节课都能按照预定的教学计划进行，可以提前准备一些额外的教学材料或活动以备不时之需。\\n2. 加强课堂管理，设立明确的课堂规则，并在课程开始时向学生说明这些规则的重要性。对于违反纪律的行为，采取适当的措施加以纠正。\\n3. 重新审视并调整教学目标和计划，使之更加符合学生的实际需求和发展水平。可以通过调研、与同行交流等方式来获取反馈，进一步优化教学内容。\\n4. 提前充分备课，对即将讲授的知识点做深入研究，准备好相关案例、习题等辅助材料，提高授课质量。同时也可以考虑参加专业培训或研讨会，不断更新自己的知识体系和教学方法。\\n\\n通过以上改进措施，该教师可以进一步提升教学质量，更好地满足学生的学习需求，促进其职业发展。")
                .setTeacherName("张三");*/
        XWPFDocument document = new AiReportExporter().generateReport(analysis);
        File file = new File("D:\\Programming\\projects\\evaluate-system\\ai报告.docx");
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(file));
        document.write(stream);
        stream.close();
    }

}
