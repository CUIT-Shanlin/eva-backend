package edu.cuit.app.resolver;

import edu.cuit.app.poi.course.csv.CourseCsvResolver;
import edu.cuit.app.poi.course.csv.ExperimentalCourseCsvResolver;
import edu.cuit.client.bo.CourseExcelBO;
import com.alibaba.cola.exception.BizException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExperimentalCourseCsvResolverTest {

    @Test
    void shouldExposeDedicatedExperimentalTemplateSchema() throws IOException {
        String csv = """
                course_name,teacher_name,prof_title,start_time,end_time,day,weeks,classroom,course_class
                数字电路实验,李老师,,5,6,3,1-16周,实验楼201,计科2023级1班
                """;

        ExperimentalCourseCsvResolver resolver = new ExperimentalCourseCsvResolver(
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
        ), ExperimentalCourseCsvResolver.orderedHeaders());
        assertEquals(
                "course_name,teacher_name,prof_title,start_time,end_time,day,weeks,classroom,course_class",
                ExperimentalCourseCsvResolver.templateHeader()
        );
        assertEquals(
                ExperimentalCourseCsvResolver.templateHeader(),
                readTemplateFile("experimental_course_import_template.csv")
        );
        assertEquals(1, results.size());
    }

    @Test
    void shouldResolveExperimentalCourseCsv() {
        String csv = """
                course_name,teacher_name,prof_title,start_time,end_time,day,weeks,classroom,course_class
                数字电路实验,李老师,,5,6,3,1-16周,实验楼201,计科2023级1班
                """;

        List<CourseExcelBO> results = CourseCsvResolver.resolveData(
                CourseCsvResolver.Strategy.EXPERIMENTAL_COURSE,
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
        );

        assertEquals(1, results.size());
        CourseExcelBO course = results.get(0);
        assertEquals("数字电路实验", course.getCourseName());
        assertEquals("李老师", course.getTeacherName());
        assertEquals(null, course.getProfTitle());
        assertEquals(5, course.getStartTime());
        assertEquals(6, course.getEndTime());
        assertEquals(3, course.getDay());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16), course.getWeeks());
        assertEquals("实验楼201", course.getClassroom());
        assertEquals("计科2023级1班", course.getCourseClass());
    }

    @Test
    void shouldRejectInvalidWeeks() {
        String csv = """
                course_name,teacher_name,prof_title,start_time,end_time,day,weeks,classroom,course_class
                数字电路实验,李老师,,5,6,3,abc,实验楼201,计科2023级1班
                """;

        BizException exception = assertThrows(BizException.class, () -> CourseCsvResolver.resolveData(
                CourseCsvResolver.Strategy.EXPERIMENTAL_COURSE,
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
        ));

        assertEquals("第2行的weeks格式不正确", exception.getMessage());
    }

    @Test
    void shouldRejectEndTimeBeforeStartTime() {
        String csv = """
                course_name,teacher_name,prof_title,start_time,end_time,day,weeks,classroom,course_class
                数字电路实验,李老师,,6,5,3,1-16周,实验楼201,计科2023级1班
                """;

        BizException exception = assertThrows(BizException.class, () -> CourseCsvResolver.resolveData(
                CourseCsvResolver.Strategy.EXPERIMENTAL_COURSE,
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
        ));

        assertEquals("第2行的end_time不能小于start_time", exception.getMessage());
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
