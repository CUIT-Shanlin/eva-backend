package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.AssignEvaTeachersCommand;
import edu.cuit.client.dto.cmd.course.AlignTeacherCmd;

import java.util.Map;
import java.util.Objects;

/**
 * 课程写侧：分配听课/评教老师入口用例（保持行为不变：不在入口用例层新增校验/异常转换）。
 *
 * <p>说明：用于让旧 gateway 退化为“仅事务边界 + 委托调用”的壳。</p>
 */
public class AssignTeacherGatewayEntryUseCase {
    private final AssignEvaTeachersUseCase useCase;

    public AssignTeacherGatewayEntryUseCase(AssignEvaTeachersUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    public Map<String, Map<Integer, Integer>> assignTeacher(Integer semId, AlignTeacherCmd alignTeacherCmd) {
        return useCase.execute(new AssignEvaTeachersCommand(
                semId,
                alignTeacherCmd.getId(),
                alignTeacherCmd.getEvaTeacherIdList()
        ));
    }
}
