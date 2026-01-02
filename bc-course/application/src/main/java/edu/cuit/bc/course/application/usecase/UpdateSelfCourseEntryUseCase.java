package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.port.UpdateSelfCoursePort;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeInfoCO;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 课程写侧：教师自助改课入口用例（保持行为不变：不在用例层新增校验/异常转换）。
 */
public class UpdateSelfCourseEntryUseCase {
    private final UpdateSelfCoursePort port;

    public UpdateSelfCourseEntryUseCase(UpdateSelfCoursePort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public Map<String, Map<Integer, Integer>> updateSelfCourse(String userName, SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeInfoCO> timeList) {
        return port.updateSelfCourse(userName, selfTeachCourseCO, timeList);
    }
}

