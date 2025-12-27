package edu.cuit.bc.course.domain;

/**
 * 课表导入异常（课程 BC 写模型异常）。
 */
public class ImportCourseFileException extends RuntimeException {
    public ImportCourseFileException(String message) {
        super(message);
    }
}

