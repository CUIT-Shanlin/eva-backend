package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.RolePermissionAssignmentPort;
import java.util.List;

/**
 * 角色权限分配用例（写模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class AssignRolePermsUseCase {
    private final RolePermissionAssignmentPort assignmentPort;

    public AssignRolePermsUseCase(RolePermissionAssignmentPort assignmentPort) {
        this.assignmentPort = assignmentPort;
    }

    public void execute(Integer roleId, List<Integer> menuIds) {
        assignmentPort.assignPerms(roleId, menuIds);
    }
}

