package edu.cuit.bc.evaluation.application.usecase;

import edu.cuit.bc.evaluation.application.port.EvaTemplatePagingQueryPort;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaTemplateEntity;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 评教模板读侧查询用例（QueryUseCase）。
 *
 * <p>当前阶段仅做“用例归位 + 委托壳”，不改变任何业务语义；
 * 旧入口仍保留 {@code @CheckSemId} 等触发点。</p>
 */
public class EvaTemplateQueryUseCase {
    private final EvaTemplatePagingQueryPort evaTemplatePagingQueryPort;

    public EvaTemplateQueryUseCase(EvaTemplatePagingQueryPort evaTemplatePagingQueryPort) {
        this.evaTemplatePagingQueryPort = Objects.requireNonNull(evaTemplatePagingQueryPort, "evaTemplatePagingQueryPort");
    }

    public PaginationQueryResultCO<EvaTemplateCO> pageEvaTemplateAsPaginationQueryResult(
            Integer semId,
            PagingQuery<GenericConditionalQuery> query
    ) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        PaginationResultEntity<EvaTemplateEntity> page = evaTemplatePagingQueryPort.pageEvaTemplate(semId, query);
        List<EvaTemplateCO> results = new ArrayList<>();
        for (int i = 0; i < page.getRecords().size(); i++) {
            EvaTemplateCO evaTemplateCO = new EvaTemplateCO();
            evaTemplateCO.setId(page.getRecords().get(i).getId());
            evaTemplateCO.setName(page.getRecords().get(i).getName());
            evaTemplateCO.setDescription(page.getRecords().get(i).getDescription());
            evaTemplateCO.setUpdateTime(page.getRecords().get(i).getUpdateTime().format(fmt));
            evaTemplateCO.setCreateTime(page.getRecords().get(i).getCreateTime().format(fmt));
            evaTemplateCO.setProps(page.getRecords().get(i).getProps());
            evaTemplateCO.setIsDefault(page.getRecords().get(i).getIsDefault());

            results.add(evaTemplateCO);
        }

        PaginationQueryResultCO<EvaTemplateCO> pageCO = new PaginationQueryResultCO<>();
        pageCO.setCurrent(page.getCurrent())
                .setSize(page.getSize())
                .setTotal(page.getTotal())
                .setRecords(results);
        return pageCO;
    }
}
