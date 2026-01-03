package edu.cuit.bc.course.application.port;

/**
 * 课程写侧：新增课程端口（渐进式重构：委托既有 legacy gateway，保持行为不变）。
 */
public interface AddCoursePort {
    void addCourse(Integer semId);
}
