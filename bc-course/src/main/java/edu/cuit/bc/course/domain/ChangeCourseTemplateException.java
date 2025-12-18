package edu.cuit.bc.course.domain;

import java.util.Collections;
import java.util.List;

/**
 * 批量切换课程模板异常（课程 BC 写模型异常）。
 */
public class ChangeCourseTemplateException extends RuntimeException {
    private final List<Integer> lockedCourseIds;

    public ChangeCourseTemplateException(String message) {
        super(message);
        this.lockedCourseIds = List.of();
    }

    private ChangeCourseTemplateException(String message, List<Integer> lockedCourseIds) {
        super(message);
        this.lockedCourseIds = lockedCourseIds == null ? List.of() : List.copyOf(lockedCourseIds);
    }

    public static ChangeCourseTemplateException templateLocked(List<Integer> lockedCourseIds) {
        return new ChangeCourseTemplateException(
                "部分课程已评教过，模板已锁定，无法切换，课程ID：" + lockedCourseIds,
                lockedCourseIds
        );
    }

    public static ChangeCourseTemplateException templateLocked(Integer courseId) {
        return templateLocked(List.of(courseId));
    }

    public List<Integer> getLockedCourseIds() {
        return Collections.unmodifiableList(lockedCourseIds);
    }
}
