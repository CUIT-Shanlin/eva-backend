package edu.cuit.bc.evaluation.application.port;

/**
 * 评教统计导出端口（读侧）。
 *
 * <p>用于把“导出实现（Excel/Word/CSV 等）”从用例中隔离到可替换的端口适配器上，
 * 过渡期保持行为不变，只做依赖归位与解耦。</p>
 */
public interface EvaStatisticsExportPort {
    /**
     * 导出某学期的评教记录统计文件。
     *
     * @param semId 学期 id
     * @return excel 文件二进制数据，content-type: application/vnd.ms-excel
     */
    byte[] exportEvaStatistics(Integer semId);
}
