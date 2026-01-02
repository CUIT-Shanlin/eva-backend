package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.port.CourseScheduleQueryPort;
import edu.cuit.client.dto.clientobject.course.SingleCourseCO;
import edu.cuit.client.dto.query.CourseQuery;

import java.util.List;
import java.util.Objects;

/**
 * 课程读侧查询用例（起步：从旧入口归位“纯查询方法簇”，保持行为不变）。
 */
public class CourseQueryUseCase {
    private final CourseScheduleQueryPort queryPort;

    public CourseQueryUseCase(CourseScheduleQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "queryPort");
    }

    public List<List<Integer>> courseNum(Integer week, Integer semId) {
        return queryPort.getWeekCourses(semId, week);
    }

    public List<SingleCourseCO> courseTimeDetail(Integer semId, CourseQuery courseQuery) {
        return queryPort.getPeriodInfo(semId, courseQuery);
    }

    public String getDate(Integer semId, Integer week, Integer day) {
        return queryPort.getDate(semId, week, day);
    }
}

