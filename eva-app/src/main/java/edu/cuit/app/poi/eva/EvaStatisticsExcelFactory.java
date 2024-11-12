package edu.cuit.app.poi.eva;

import org.apache.poi.ss.usermodel.Workbook;

/**
 * 评教数据excel工厂类
 */
public class EvaStatisticsExcelFactory {

    /**
     * 创建统计excel表
     * @param semId 学期id
     * @return 二进制数据
     */
    public static Workbook createExcelData(Integer semId) {
        EvaStatisticsExporter evaStatisticsExporter = new EvaStatisticsExporter();
        evaStatisticsExporter.setSemesterId(semId);
        FillAverageScoreExporterDecorator fillScoreExporterDecorator = new FillAverageScoreExporterDecorator(evaStatisticsExporter);
        fillScoreExporterDecorator.process();

        return fillScoreExporterDecorator.getWorkbook();
    }

}
