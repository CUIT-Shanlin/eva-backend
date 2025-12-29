package edu.cuit.infra.bcevaluation.query;

import edu.cuit.app.poi.eva.EvaStatisticsExcelFactory;
import edu.cuit.bc.evaluation.application.port.EvaStatisticsExportPort;

/**
 * 评教统计导出端口适配器（基础设施端）。
 *
 * <p>过渡期保持行为不变：复用既有 {@link EvaStatisticsExcelFactory} 的导出实现。</p>
 */
public class EvaStatisticsExportPortImpl implements EvaStatisticsExportPort {
    @Override
    public byte[] exportEvaStatistics(Integer semId) {
        return EvaStatisticsExcelFactory.createExcelData(semId);
    }
}
