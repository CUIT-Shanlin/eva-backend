package edu.cuit.app.poi.course.csv;

import edu.cuit.client.bo.CourseExcelBO;

import java.io.InputStream;
import java.util.List;

public final class TheoryCourseCsvResolver extends CourseCsvResolverStrategy<TheoryCourseCsvRow> {

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

    public TheoryCourseCsvResolver(InputStream csvFileStream) {
        super(csvFileStream);
    }

    public static List<String> orderedHeaders() {
        return List.copyOf(ORDERED_HEADERS);
    }

    public static String templateHeader() {
        return String.join(",", ORDERED_HEADERS);
    }

    @Override
    protected Class<TheoryCourseCsvRow> rowType() {
        return TheoryCourseCsvRow.class;
    }

    @Override
    protected List<String> orderedHeaderColumns() {
        return ORDERED_HEADERS;
    }

    @Override
    protected CourseExcelBO toCourseExcelBO(TheoryCourseCsvRow row, int lineNumber) {
        return toStandardCourseExcelBO(row, lineNumber);
    }
}
