package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.ImportCourseFileCommand;
import edu.cuit.bc.course.application.port.ImportCourseFileRepository;
import edu.cuit.bc.course.domain.ImportCourseFileException;

import java.util.Map;
import java.util.Objects;

/**
 * 课表导入用例。
 *
 * <p>说明：当前阶段主要目标是把“导入课表”的核心写操作从旧 gateway/service 中收敛到 bc-course；
 * 副作用（消息通知、撤回任务等）由上层通过事件交给 bc-messaging 处理。</p>
 */
public class ImportCourseFileUseCase {
    private final ImportCourseFileRepository repository;

    public ImportCourseFileUseCase(ImportCourseFileRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Map<String, Map<Integer, Integer>> execute(ImportCourseFileCommand command) {
        Objects.requireNonNull(command, "command");
        if (command.semester() == null) {
            throw new ImportCourseFileException("学期信息不能为空");
        }
        if (command.type() == null) {
            throw new ImportCourseFileException("课表类型不能为空");
        }
        if (command.courseExce() == null) {
            throw new ImportCourseFileException("课表数据不能为空");
        }
        return repository.importCourseFile(command);
    }
}

