package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.port.CourseDetailQueryPort;
import edu.cuit.client.dto.clientobject.course.SingleCourseDetailCO;

import java.util.Objects;
import java.util.Optional;

/**
 * 课程读侧：单节课详情查询用例（起步：从旧入口归位，保持行为不变）。
 */
public class CourseDetailQueryUseCase {
    private final CourseDetailQueryPort queryPort;

    public CourseDetailQueryUseCase(CourseDetailQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "queryPort");
    }

    public Optional<SingleCourseDetailCO> getCourseDetail(Integer semId, Integer id) {
        return queryPort.getCourseDetail(semId, id);
    }
}

