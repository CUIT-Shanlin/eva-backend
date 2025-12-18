package edu.cuit.bc.course.domain;

/**
 * 改课异常（课程 BC 写模型异常）。
 */
public class UpdateSingleCourseException extends RuntimeException {
    public UpdateSingleCourseException(String message) {
        super(message);
    }
}

