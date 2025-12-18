package edu.cuit.bc.template.application.port;

/**
 * 课程模板锁定查询端口。
 *
 * <p>用于判断：某课程在某学期是否已经产生评教数据（从而模板锁定）。</p>
 */
public interface CourseTemplateLockQueryPort {
    boolean isLocked(Integer courseId, Integer semesterId);
}

