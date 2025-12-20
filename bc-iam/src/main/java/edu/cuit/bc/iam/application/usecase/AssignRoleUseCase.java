package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.UserRoleAssignmentPort;

import java.util.List;

/**
 * 分配用户角色用例（写模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class AssignRoleUseCase {
    private final UserRoleAssignmentPort assignmentPort;

    public AssignRoleUseCase(UserRoleAssignmentPort assignmentPort) {
        this.assignmentPort = assignmentPort;
    }

    public void execute(Integer userId, List<Integer> roleId) {
        assignmentPort.assignRole(userId, roleId);
    }
}

