package edu.cuit.infra.bcevaluation.query;

/**
 * 评教读侧 QueryRepo（将复杂查询与组装从 gateway 中抽离）。
 *
 * <p>注意：此处是“渐进式结构化”的第一步，暂不引入 CQRS 投影表，保持行为不变。</p>
 */
public interface EvaQueryRepo extends EvaStatisticsQueryRepo, EvaRecordQueryRepo, EvaTaskQueryRepo, EvaTemplateQueryRepo {
}
