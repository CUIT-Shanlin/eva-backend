package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.port.TimeCourseQueryPort;
import edu.cuit.client.dto.clientobject.course.RecommendCourseCO;
import edu.cuit.client.dto.query.condition.MobileCourseQuery;

import java.util.List;
import java.util.Objects;

/**
 * 课程读侧：指定时间段课程查询用例（旧入口保留登录态解析；用例只接收 username 并编排查询，保持行为不变）。
 */
public class TimeCourseQueryUseCase {
    private final TimeCourseQueryPort queryPort;

    public TimeCourseQueryUseCase(TimeCourseQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "queryPort");
    }

    public List<RecommendCourseCO> getTimeCourse(Integer semId, MobileCourseQuery courseQuery, String userName) {
        return queryPort.getTimeCourse(semId, courseQuery, userName);
    }
}

