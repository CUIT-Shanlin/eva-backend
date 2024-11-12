package edu.cuit.app.poi.eva;

import com.alibaba.cola.exception.SysException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 评教数据excel工厂类
 */
@Slf4j
public class EvaStatisticsExcelFactory {

    /**
     * 创建统计excel表
     * @param semId 学期id
     * @return 二进制数据
     */
    public static byte[] createExcelData(Integer semId) {
        EvaStatisticsExporter exporter = getFillUserStatisticsExporterDecorator(semId);

        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        byte[] data;
        try {
            exporter.getWorkbook().write(byteOutputStream);
            data = byteOutputStream.toByteArray();
            byteOutputStream.close();
        } catch (IOException e) {
            SysException e1 = new SysException("文件导出异常，请联系管理员");
            log.error("发生系统异常",e);
            throw e1;
        }
        return data;
    }

    private static EvaStatisticsExporter getFillUserStatisticsExporterDecorator(Integer semId) {
        EvaStatisticsExporter evaStatisticsExporter = new EvaStatisticsExporter(semId);
        FillAverageScoreExporterDecorator fillScoreExporterDecorator = new FillAverageScoreExporterDecorator(evaStatisticsExporter);
        FillEvaRecordExporterDecorator fillEvaRecordExporterDecorator = new FillEvaRecordExporterDecorator(fillScoreExporterDecorator);
        FillUserStatisticsExporterDecorator fillUserStatisticsDecorator = new FillUserStatisticsExporterDecorator(fillEvaRecordExporterDecorator);
        fillUserStatisticsDecorator.process();
        return fillUserStatisticsDecorator;
    }

}
