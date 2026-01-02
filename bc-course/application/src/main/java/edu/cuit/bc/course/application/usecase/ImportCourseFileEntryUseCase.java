package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.port.ImportCourseFilePort;
import edu.cuit.client.bo.CourseExcelBO;
import edu.cuit.client.dto.clientobject.SemesterCO;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 课程写侧：导入课表文件入口用例（保持行为不变：不在用例层新增校验/异常转换）。
 */
public class ImportCourseFileEntryUseCase {
    private final ImportCourseFilePort port;

    public ImportCourseFileEntryUseCase(ImportCourseFilePort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public Map<String, Map<Integer, Integer>> importCourseFile(Map<String, List<CourseExcelBO>> courseExce, SemesterCO semester, Integer type) {
        return port.importCourseFile(courseExce, semester, type);
    }
}

