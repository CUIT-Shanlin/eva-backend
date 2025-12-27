package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.port.CourseImportedQueryPort;
import edu.cuit.client.dto.data.Term;

/**
 * 判断学期课表是否已导入用例（读模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class IsCourseImportedUseCase {
    private final CourseImportedQueryPort queryPort;

    public IsCourseImportedUseCase(CourseImportedQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public Boolean execute(Integer type, Term term) {
        return queryPort.isImported(type, term);
    }
}

