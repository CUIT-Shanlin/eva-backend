package edu.cuit.bc.iam.application.usecase;

import edu.cuit.domain.gateway.DepartmentGateway;
import java.util.List;

/**
 * 院系查询用例（读模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离用例），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class DepartmentQueryUseCase {
    private final DepartmentGateway departmentGateway;

    public DepartmentQueryUseCase(DepartmentGateway departmentGateway) {
        this.departmentGateway = departmentGateway;
    }

    public List<String> all() {
        return departmentGateway.getAll();
    }
}
