package edu.cuit.bc.template.application;

import edu.cuit.bc.template.application.port.CourseTemplateLockQueryPort;
import edu.cuit.bc.template.domain.TemplateLockedException;

import java.util.Objects;

/**
 * 课程模板锁定服务（用例级规则）。
 *
 * <p>业务规则：只要有人对该课程发生过评教（产生评教记录），则该课程在该学期的模板即锁定，不允许切换。</p>
 */
public class CourseTemplateLockService {
    private final CourseTemplateLockQueryPort queryPort;

    public CourseTemplateLockService(CourseTemplateLockQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "queryPort");
    }

    public void assertCanChangeTemplate(Integer courseId, Integer semesterId) {
        if (courseId == null) {
            return;
        }
        if (queryPort.isLocked(courseId, semesterId)) {
            throw new TemplateLockedException("课程已评教过，模板已锁定，无法切换");
        }
    }
}
