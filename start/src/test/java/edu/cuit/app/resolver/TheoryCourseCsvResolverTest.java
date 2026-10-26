package edu.cuit.app.resolver;

import edu.cuit.app.poi.course.csv.CourseCsvResolver;
import edu.cuit.app.poi.course.csv.TheoryCourseCsvResolver;
import edu.cuit.client.bo.CourseExcelBO;
import com.alibaba.cola.exception.BizException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TheoryCourseCsvResolverTest {

    @Test
    void shouldExposeDedicatedTheoryTemplateSchema() throws IOException {
        String csv = """
                course_name,teacher_name,prof_title,start_time,end_time,day,weeks,classroom,course_class
                高等数学,王老师,教授,1,2,1,1-16周,一教101,信安2023级2班
                """;

        TheoryCourseCsvResolver resolver = new TheoryCourseCsvResolver(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
        );
        List<CourseExcelBO> results = resolver.readData();

        assertEquals(List.of(
                "course_name",
                "teacher_name",
                "prof_title",
                "start_time",
                "end_time",
                "day",
                "weeks",
                "classroom",
                "course_class"
        ), TheoryCourseCsvResolver.orderedHeaders());
        assertEquals(
                "course_name,teacher_name,prof_title,start_time,end_time,day,weeks,classroom,course_class",
                TheoryCourseCsvResolver.templateHeader()
        );
        assertEquals(
                TheoryCourseCsvResolver.templateHeader(),
                readTemplateFile("theory_course_import_template.csv")
        );
        assertEquals(1, results.size());
    }

    @Test
    void shouldResolveTheoryCourseCsv() {
        String csv = """
                course_name,teacher_name,prof_title,start_time,end_time,day,weeks,classroom,course_class
                高等数学,王老师,教授,1,2,1,1-16周,一教101,信安2023级2班
                """;

        List<CourseExcelBO> results = CourseCsvResolver.resolveData(
                CourseCsvResolver.Strategy.THEORY_COURSE,
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
        );

        assertEquals(1, results.size());
        CourseExcelBO course = results.get(0);
        assertEquals("高等数学", course.getCourseName());
        assertEquals("王老师", course.getTeacherName());
        assertEquals("教授", course.getProfTitle());
        assertEquals(1, course.getStartTime());
        assertEquals(2, course.getEndTime());
        assertEquals(1, course.getDay());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16), course.getWeeks());
        assertEquals("一教101", course.getClassroom());
        assertEquals("信安2023级2班", course.getCourseClass());
    }

    @Test
    void shouldRejectUnexpectedHeaderOrder() {
        String csv = """
                teacher_name,course_name,prof_title,start_time,end_time,day,weeks,classroom,course_class
                王老师,高等数学,教授,1,2,1,1-16周,一教101,信安2023级2班
                """;

        BizException exception = assertThrows(BizException.class, () -> CourseCsvResolver.resolveData(
                CourseCsvResolver.Strategy.THEORY_COURSE,
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
        ));

        assertEquals("CSV表头不符合模板要求", exception.getMessage());
    }

    @Test
    void shouldRejectMissingRequiredCourseName() {
        String csv = """
                course_name,teacher_name,prof_title,start_time,end_time,day,weeks,classroom,course_class
                ,王老师,教授,1,2,1,1-16周,一教101,信安2023级2班
                """;

        BizException exception = assertThrows(BizException.class, () -> CourseCsvResolver.resolveData(
                CourseCsvResolver.Strategy.THEORY_COURSE,
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
        ));

        assertEquals("第2行的course_name不能为空", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidDay() {
        String csv = """
                course_name,teacher_name,prof_title,start_time,end_time,day,weeks,classroom,course_class
                高等数学,王老师,教授,1,2,8,1-16周,一教101,信安2023级2班
                """;

        BizException exception = assertThrows(BizException.class, () -> CourseCsvResolver.resolveData(
                CourseCsvResolver.Strategy.THEORY_COURSE,
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
        ));

        assertEquals("第2行的day必须在1到7之间", exception.getMessage());
    }

    @Test
    void shouldSkipBlankRows() {
        String csv = """
                course_name,teacher_name,prof_title,start_time,end_time,day,weeks,classroom,course_class
                高等数学,王老师,教授,1,2,1,1-16周,一教101,信安2023级2班
                ,,,,,,,,

                线性代数,张老师,副教授,3,4,2,1-8单周,二教203,信安2023级2班
                """;

        List<CourseExcelBO> results = CourseCsvResolver.resolveData(
                CourseCsvResolver.Strategy.THEORY_COURSE,
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
        );

        assertEquals(2, results.size());
        assertEquals("高等数学", results.get(0).getCourseName());
        assertEquals("线性代数", results.get(1).getCourseName());
    }

    @Test
    void shouldWrapIoExceptionAsBizException() {
        BizException exception = assertThrows(BizException.class, () -> CourseCsvResolver.resolveData(
                CourseCsvResolver.Strategy.THEORY_COURSE,
                new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("broken stream");
                    }
                }
        ));

        assertEquals("读取csv出错，请联系管理员", exception.getMessage());
    }

    @Test
    void shouldRejectMissingStrategy() {
        String csv = """
                course_name,teacher_name,prof_title,start_time,end_time,day,weeks,classroom,course_class
                高等数学,王老师,教授,1,2,1,1-16周,一教101,信安2023级2班
                """;

        BizException exception = assertThrows(BizException.class, () -> CourseCsvResolver.resolveData(
                null,
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
        ));

        assertEquals("读取策略不存在，请联系管理员", exception.getMessage());
    }

    private String readTemplateFile(String fileName) throws IOException {
        Path current = Path.of("").toAbsolutePath();
        Path templatePath = current.resolve("data").resolve(fileName);
        if (!Files.exists(templatePath)) {
            templatePath = current.resolve("..").normalize().resolve("data").resolve(fileName);
        }
        return Files.readString(templatePath, StandardCharsets.UTF_8).trim();
    }
}
