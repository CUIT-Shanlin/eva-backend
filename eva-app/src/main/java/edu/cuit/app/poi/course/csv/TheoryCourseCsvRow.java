package edu.cuit.app.poi.course.csv;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public final class TheoryCourseCsvRow implements CourseCsvRowView {

    @JsonProperty("course_name")
    private String courseName;

    @JsonProperty("teacher_name")
    private String teacherName;

    @JsonProperty("prof_title")
    private String profTitle;

    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("end_time")
    private String endTime;

    @JsonProperty("day")
    private String day;

    @JsonProperty("weeks")
    private String weeks;

    @JsonProperty("classroom")
    private String classroom;

    @JsonProperty("course_class")
    private String courseClass;
}
