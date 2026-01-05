package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.ImportCourseFileCommand;
import edu.cuit.client.bo.CourseExcelBO;
import edu.cuit.client.dto.clientobject.SemesterCO;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 课程写侧：导入课表入口用例（保持行为不变：不在入口用例层新增校验/异常转换）。
 *
 * <p>说明：用于让旧 gateway 退化为“仅事务边界 + 异常转换 + 委托调用”的壳。</p>
 */
public class ImportCourseFileGatewayEntryUseCase {
    private final ImportCourseFileUseCase useCase;

    public ImportCourseFileGatewayEntryUseCase(ImportCourseFileUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    public Map<String, Map<Integer, Integer>> importCourseFile(
            Map<String, List<CourseExcelBO>> courseExce,
            SemesterCO semester,
            Integer type
    ) {
        return useCase.execute(new ImportCourseFileCommand(courseExce, semester, type));
    }
}
