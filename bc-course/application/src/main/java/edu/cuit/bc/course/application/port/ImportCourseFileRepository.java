package edu.cuit.bc.course.application.port;

import edu.cuit.bc.course.application.model.ImportCourseFileCommand;

import java.util.Map;

/**
 * 课表导入端口（由基础设施层实现）。
 */
public interface ImportCourseFileRepository {
    Map<String, Map<Integer, Integer>> importCourseFile(ImportCourseFileCommand command);
}

