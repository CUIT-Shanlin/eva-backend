package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.AssignEvaTeachersCommand;
import edu.cuit.bc.course.application.port.AssignEvaTeachersRepository;
import edu.cuit.bc.course.domain.AssignEvaTeachersException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 分配听课/评教老师用例。
 */
public class AssignEvaTeachersUseCase {
    private final AssignEvaTeachersRepository repository;

    public AssignEvaTeachersUseCase(AssignEvaTeachersRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Map<String, Map<Integer, Integer>> execute(AssignEvaTeachersCommand command) {
        Objects.requireNonNull(command, "command");
        Integer semesterId = command.semesterId();
        Integer courInfId = command.courInfId();
        List<Integer> evaTeacherIdList = command.evaTeacherIdList();

        if (semesterId == null) {
            throw new AssignEvaTeachersException("学期ID不能为空");
        }
        if (courInfId == null) {
            throw new AssignEvaTeachersException("课次ID不能为空");
        }
        if (evaTeacherIdList == null || evaTeacherIdList.isEmpty()) {
            throw new AssignEvaTeachersException("评教老师列表不能为空");
        }
        if (evaTeacherIdList.stream().anyMatch(Objects::isNull)) {
            throw new AssignEvaTeachersException("评教老师列表不能包含空值");
        }

        return repository.assign(semesterId, courInfId, evaTeacherIdList);
    }
}
