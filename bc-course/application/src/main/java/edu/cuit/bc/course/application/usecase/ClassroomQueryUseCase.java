package edu.cuit.bc.course.application.usecase;

import edu.cuit.domain.gateway.ClassroomGateway;
import java.util.List;

/**
 * 教室查询用例（读模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离用例），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class ClassroomQueryUseCase {
    private final ClassroomGateway classroomGateway;

    public ClassroomQueryUseCase(ClassroomGateway classroomGateway) {
        this.classroomGateway = classroomGateway;
    }

    public List<String> getAll() {
        return classroomGateway.getAll();
    }
}
