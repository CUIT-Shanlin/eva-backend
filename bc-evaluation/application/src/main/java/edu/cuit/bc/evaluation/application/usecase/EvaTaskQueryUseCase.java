package edu.cuit.bc.evaluation.application.usecase;

import edu.cuit.bc.evaluation.application.port.EvaTaskPagingQueryPort;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskBaseInfoCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaTaskConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;

import java.util.List;
import java.util.Objects;

/**
 * 评教任务读侧查询用例（QueryUseCase）。
 *
 * <p>当前阶段仅做“用例归位 + 委托壳”，不改变任何业务语义；
 * 旧入口仍保留 {@code @CheckSemId} 等触发点。</p>
 */
public class EvaTaskQueryUseCase {
    private final EvaTaskPagingQueryPort evaTaskPagingQueryPort;

    public EvaTaskQueryUseCase(EvaTaskPagingQueryPort evaTaskPagingQueryPort) {
        this.evaTaskPagingQueryPort = Objects.requireNonNull(evaTaskPagingQueryPort, "evaTaskPagingQueryPort");
    }

    public PaginationQueryResultCO<EvaTaskBaseInfoCO> pageEvaUnfinishedTaskAsPaginationQueryResult(
            Integer semId,
            PagingQuery<EvaTaskConditionalQuery> query
    ) {
        PaginationResultEntity<EvaTaskEntity> page = evaTaskPagingQueryPort.pageEvaUnfinishedTask(semId, query);
        List<EvaTaskBaseInfoCO> results = page.getRecords().stream()
                .map(EvaTaskQueryUseCase::evaTaskEntityToEvaBaseCO)
                .toList();

        PaginationQueryResultCO<EvaTaskBaseInfoCO> pageCO = new PaginationQueryResultCO<>();
        pageCO.setCurrent(page.getCurrent())
                .setSize(page.getSize())
                .setTotal(page.getTotal())
                .setRecords(results);
        return pageCO;
    }

    private static EvaTaskBaseInfoCO evaTaskEntityToEvaBaseCO(EvaTaskEntity evaTaskEntity) {
        if (evaTaskEntity == null) {
            return null;
        }

        EvaTaskBaseInfoCO evaTaskBaseInfoCO = new EvaTaskBaseInfoCO();

        if (evaTaskEntity.getId() != null) {
            evaTaskBaseInfoCO.setId(evaTaskEntity.getId().longValue());
        }
        if (evaTaskEntity.getStatus() != null) {
            evaTaskBaseInfoCO.setStatus(evaTaskEntity.getStatus().longValue());
        }
        evaTaskBaseInfoCO.setCreateTime(evaTaskEntity.getCreateTime());
        evaTaskBaseInfoCO.setUpdateTime(evaTaskEntity.getUpdateTime());

        evaTaskBaseInfoCO.setEvaTeacherName(evaTaskEntity.getTeacher().getName());
        evaTaskBaseInfoCO.setTeacherName(evaTaskEntity.getCourInf().getCourseEntity().getTeacher().getName());
        evaTaskBaseInfoCO.setCourseName(evaTaskEntity.getCourInf().getCourseEntity().getSubjectEntity().getName());

        return evaTaskBaseInfoCO;
    }
}
