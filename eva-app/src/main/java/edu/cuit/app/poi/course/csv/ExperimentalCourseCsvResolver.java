package edu.cuit.app.poi.course.csv;

import edu.cuit.client.bo.CourseExcelBO;

import java.io.InputStream;
import java.util.List;

public final class ExperimentalCourseCsvResolver extends CourseCsvResolverStrategy<ExperimentalCourseCsvRow> {

    private static final List<String> ORDERED_HEADERS = List.of(
            "course_name",
            "teacher_name",
            "prof_title",
            "start_time",
            "end_time",
            "day",
            "weeks",
            "classroom",
            "course_class"
    );

    public ExperimentalCourseCsvResolver(InputStream csvFileStream) {
        super(csvFileStream);
    }

    public static List<String> orderedHeaders() {
        return List.copyOf(ORDERED_HEADERS);
    }

    public static String templateHeader() {
        return String.join(",", ORDERED_HEADERS);
    }

    @Override
    protected Class<ExperimentalCourseCsvRow> rowType() {
        return ExperimentalCourseCsvRow.class;
    }

    @Override
    protected List<String> orderedHeaderColumns() {
        return ORDERED_HEADERS;
    }

    @Override
    protected CourseExcelBO toCourseExcelBO(ExperimentalCourseCsvRow row, int lineNumber) {
        return toStandardCourseExcelBO(row, lineNumber);
    }
}
