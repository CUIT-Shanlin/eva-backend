package edu.cuit.app.poi.course.csv;

import com.alibaba.cola.exception.BizException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import edu.cuit.client.bo.CourseExcelBO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 课程 CSV 解析抽象模板
 */
abstract class CourseCsvResolverStrategy<T extends CourseCsvRowView> {

    private static final CsvMapper CSV_MAPPER = CsvMapper.builder().build();

    protected final InputStream csvFileStream;

    protected CourseCsvResolverStrategy(InputStream csvFileStream) {
        this.csvFileStream = csvFileStream;
    }

    public List<CourseExcelBO> readData() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvFileStream, StandardCharsets.UTF_8))) {
            validateHeader(reader.readLine());
            try (MappingIterator<T> iterator = CSV_MAPPER.readerFor(rowType())
                    .with(buildSchema(orderedHeaderColumns()))
                    .readValues(reader)) {
                List<CourseExcelBO> results = new java.util.ArrayList<>();
                int lineNumber = 1;
                while (iterator.hasNextValue()) {
                    lineNumber++;
                    T row = iterator.nextValue();
                    if (isBlankRow(row)) {
                        continue;
                    }
                    results.add(toCourseExcelBO(row, lineNumber));
                }
                return results;
            }
        }
    }

    protected abstract Class<T> rowType();

    protected abstract List<String> orderedHeaderColumns();

    protected abstract CourseExcelBO toCourseExcelBO(T row, int lineNumber);

    protected final String expectedHeaderLine() {
        return String.join(",", orderedHeaderColumns());
    }

    private CsvSchema buildSchema(List<String> orderedHeaders) {
        CsvSchema.Builder builder = CsvSchema.builder();
        for (String orderedHeader : orderedHeaders) {
            builder.addColumn(orderedHeader);
        }
        return builder.build();
    }

    private void validateHeader(String headerLine) {
        if (!expectedHeaderLine().equals(stripBom(headerLine))) {
            throw new BizException("CSV表头不符合模板要求");
        }
    }

    protected final CourseExcelBO toStandardCourseExcelBO(CourseCsvRowView row, int lineNumber) {
        Integer startTime = parseInteger("start_time", row.getStartTime(), lineNumber);
        Integer endTime = parseInteger("end_time", row.getEndTime(), lineNumber);
        if (endTime < startTime) {
            throw new BizException("第" + lineNumber + "行的end_time不能小于start_time");
        }

        Integer day = parseInteger("day", row.getDay(), lineNumber);
        if (day < 1 || day > 7) {
            throw new BizException("第" + lineNumber + "行的day必须在1到7之间");
        }

        return new CourseExcelBO()
                .setCourseName(requireText("course_name", row.getCourseName(), lineNumber))
                .setTeacherName(requireText("teacher_name", row.getTeacherName(), lineNumber))
                .setProfTitle(normalize(row.getProfTitle()))
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setDay(day)
                .setWeeks(CourseCsvWeeks.resolve(requireText("weeks", row.getWeeks(), lineNumber), lineNumber))
                .setClassroom(requireText("classroom", row.getClassroom(), lineNumber))
                .setCourseClass(requireText("course_class", row.getCourseClass(), lineNumber));
    }

    private Integer parseInteger(String fieldName, String value, int lineNumber) {
        String normalized = requireText(fieldName, value, lineNumber);
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            throw new BizException("第" + lineNumber + "行的" + fieldName + "格式不正确");
        }
    }

    private String requireText(String fieldName, String value, int lineNumber) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new BizException("第" + lineNumber + "行的" + fieldName + "不能为空");
        }
        return normalized;
    }

    private boolean isBlankRow(CourseCsvRowView row) {
        return normalize(row.getCourseName()) == null
                && normalize(row.getTeacherName()) == null
                && normalize(row.getProfTitle()) == null
                && normalize(row.getStartTime()) == null
                && normalize(row.getEndTime()) == null
                && normalize(row.getDay()) == null
                && normalize(row.getWeeks()) == null
                && normalize(row.getClassroom()) == null
                && normalize(row.getCourseClass()) == null;
    }

    private String stripBom(String value) {
        if (value == null) {
            return null;
        }
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
