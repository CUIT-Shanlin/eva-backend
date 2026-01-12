package edu.cuit.bc.course.application.usecase;

import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.domain.gateway.SemesterGateway;
import java.util.List;
import java.util.Objects;

/**
 * 学期读侧查询用例（起步：从旧入口收敛为“委托用例”，保持行为不变）。
 *
 * <p>说明：当前实现仅对齐旧实现的调用链路，具体查询与异常语义仍由 {@link SemesterGateway} 承担。</p>
 */
public class SemesterQueryUseCase {
    private final SemesterGateway semesterGateway;

    public SemesterQueryUseCase(SemesterGateway semesterGateway) {
        this.semesterGateway = Objects.requireNonNull(semesterGateway, "semesterGateway");
    }

    public List<SemesterCO> all() {
        return semesterGateway.getAll();
    }

    public SemesterCO now() {
        return semesterGateway.getNow();
    }

    public SemesterCO semesterInfo(Integer id) {
        return semesterGateway.getSemesterInfo(id);
    }
}
