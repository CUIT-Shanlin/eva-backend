package edu.cuit.bc.course.domain;

/**
 * 课程不存在异常（课程 BC 领域异常）。
 */
public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(String message) {
        super(message);
    }
}

