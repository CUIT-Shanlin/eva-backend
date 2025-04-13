package edu.cuit.app.poi.ai;

import edu.cuit.client.bo.ai.AiAnalysisBO;
import edu.cuit.client.bo.ai.AiCourseSuggestionBO;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;

public class AiReportExporter {

    public XSSFWorkbook generateReport(AiAnalysisBO analysis) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // 创建样式
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle subTitleStyle = createSubTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // 课程详细报告工作表
            XSSFSheet courseSheet = workbook.createSheet("课程报告");
            generateCourseSheet(courseSheet, analysis, titleStyle, subTitleStyle, dataStyle);

            // 总体报告工作表
            XSSFSheet overallSheet = workbook.createSheet("总体报告");
            generateOverallSheet(overallSheet, analysis, titleStyle, subTitleStyle, dataStyle);

            return workbook;
        }
    }

    private void generateCourseSheet(XSSFSheet sheet, AiAnalysisBO analysis,
                                     CellStyle titleStyle, CellStyle subTitleStyle, CellStyle dataStyle) {
        int rowNum = 0;
        for (AiCourseSuggestionBO course : analysis.getCourseSuggestions()) {
            // 课程标题
            rowNum = createCourseHeader(sheet, rowNum, course, titleStyle);

            // 课程统计信息
            rowNum = createCourseStats(sheet, rowNum, course, subTitleStyle, dataStyle);

            // 课程详细分析
            rowNum = createCourseAnalysis(sheet, rowNum, course, subTitleStyle, dataStyle);

            // 创建饼图
            createCourseChart(sheet, rowNum, course);
            rowNum += 15; // 为图表留出空间
        }
    }

    private int createCourseHeader(XSSFSheet sheet, int rowNum, AiCourseSuggestionBO course, CellStyle style) {
        Row row = sheet.createRow(rowNum++);
        Cell cell = row.createCell(0);
        cell.setCellValue("课程名称：" + course.getCourseName());
        cell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 4));
        return rowNum;
    }

    private int createCourseStats(XSSFSheet sheet, int rowNum, AiCourseSuggestionBO course,
                                  CellStyle headerStyle, CellStyle dataStyle) {
        Row row = sheet.createRow(rowNum++);
        addMergedCell(sheet, row, 0,  "统计信息", headerStyle, 0, 0);
        addCell(row, 1, "被评次数：" + course.getBeEvaNumCount(), dataStyle);
        addCell(row, 3, "高分次数：" + course.getHighScoreBeEvaCount(), dataStyle);
        return rowNum;
    }

    private int createCourseAnalysis(XSSFSheet sheet, int rowNum, AiCourseSuggestionBO course,
                                     CellStyle headerStyle, CellStyle dataStyle) {
        // 优点
        Row adventRow = sheet.createRow(rowNum++);
        addMergedCell(sheet, adventRow,  0, "优点", headerStyle, 1, 4);
        addCell(adventRow, 1, course.getAdventures(), dataStyle);

        // 缺点
        Row drawRow = sheet.createRow(rowNum++);
        addMergedCell(sheet, drawRow, 0, "缺点", headerStyle, 1, 4);
        addCell(drawRow, 1, course.getDrawbacks(), dataStyle);

        // 建议
        Row suggestRow = sheet.createRow(rowNum++);
        addMergedCell(sheet, suggestRow, 0,  "改进建议", headerStyle, 1, 4);
        addCell(suggestRow, 1, course.getSuggestion(), dataStyle);

        return rowNum;
    }

    private void createCourseChart(XSSFSheet sheet, int chartRow, AiCourseSuggestionBO course) {
        // 创建辅助数据
        int dataRow = chartRow + 2;
        createChartData(sheet, dataRow, course);

        // 创建图表
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 5, chartRow, 10, chartRow + 10);

        //XSSFChart chart = (XSSFChart) drawing.createChart(anchor);
        XSSFChart chart = ((XSSFDrawing) drawing).createChart(anchor);
        // 配置图表数据
        XDDFDataSource<String> categories = XDDFDataSourcesFactory.fromStringCellRange(
                sheet, new CellRangeAddress(dataRow, dataRow+1, 5, 5));
        XDDFNumericalDataSource<Double> values = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet, new CellRangeAddress(dataRow, dataRow+1, 6, 6));
        XDDFChartData data = chart.createData(ChartTypes.PIE, null, null);
//        XDDFChartData data = new XDDFPieChartData(chart.getCTChart());
        data.addSeries(categories, values);
        chart.plot(data);
        chart.setTitleText(course.getCourseName() + " 评分分布");
    }

    private void createChartData(XSSFSheet sheet, int rowNum, AiCourseSuggestionBO course) {
        Row highRow = sheet.createRow(rowNum++);
        highRow.createCell(5).setCellValue("高分");
        highRow.createCell(6).setCellValue(course.getHighScoreBeEvaCount());

        Row otherRow = sheet.createRow(rowNum);
        otherRow.createCell(5).setCellValue("其他");
        otherRow.createCell(6).setCellValue(course.getBeEvaNumCount() - course.getHighScoreBeEvaCount());
    }

    private void generateOverallSheet(XSSFSheet sheet, AiAnalysisBO analysis,
                                      CellStyle titleStyle, CellStyle subTitleStyle, CellStyle dataStyle) {
        int rowNum = 0;

        // 标题
        Row titleRow = sheet.createRow(rowNum++);
        addMergedCell(sheet, titleRow, 0,
                analysis.getTeacherName() + " 总体教学报告", titleStyle, 0, 4);

        // 总体统计
        rowNum = createOverallStats(sheet, rowNum, analysis, subTitleStyle, dataStyle);

        // 总体分析
        rowNum = createOverallAnalysis(sheet, rowNum, analysis, subTitleStyle, dataStyle);

        // 词频统计
        rowNum = createWordFrequency(sheet, rowNum, analysis, subTitleStyle, dataStyle);

        // 总体饼图
        createOverallChart(sheet, rowNum, analysis);
    }

    private int createOverallStats(XSSFSheet sheet, int rowNum, AiAnalysisBO analysis,
                                   CellStyle headerStyle, CellStyle dataStyle) {
        Row row = sheet.createRow(rowNum++);
        addMergedCell(sheet, row, 0, "总体统计", headerStyle, 0, 0);
        addCell(row, 1, "总评次数：" + analysis.getTotalBeEvaCount(), dataStyle);
        addCell(row, 3, "总高分次数：" + analysis.getHighScoreEvaCount(), dataStyle);
        return rowNum;
    }

    private int createOverallAnalysis(XSSFSheet sheet, int rowNum, AiAnalysisBO analysis,
                                      CellStyle headerStyle, CellStyle dataStyle) {
        Row row = sheet.createRow(rowNum++);
        addMergedCell(sheet, row, 0,  "总体评价", headerStyle, 1, 4);
        addCell(row, 1, analysis.getOverallReport(), dataStyle);
        return rowNum + 2;
    }

    private int createWordFrequency(XSSFSheet sheet, int rowNum, AiAnalysisBO analysis,
                                    CellStyle headerStyle, CellStyle dataStyle) {
        Row titleRow = sheet.createRow(rowNum++);
        addMergedCell(sheet, titleRow, 0,  "高频词汇统计", headerStyle, 0, 1);

        Row headerRow = sheet.createRow(rowNum++);
        addCell(headerRow, 0, "词汇", headerStyle);
        addCell(headerRow, 1, "出现次数", headerStyle);

        for (Pair<String, Integer> pair : analysis.getWordFrequencyCount()) {
            Row row = sheet.createRow(rowNum++);
            addCell(row, 0, pair.getKey(), dataStyle);
            addCell(row, 1, pair.getValue(), dataStyle);
        }
        return rowNum;
    }

    private void createOverallChart(XSSFSheet sheet, int rowNum, AiAnalysisBO analysis) {
        // 创建辅助数据
        int dataRow = rowNum + 2;
        createOverallChartData(sheet, dataRow, analysis);

        // 创建图表
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 5, rowNum, 10, rowNum + 10);
        XSSFChart chart = ((XSSFDrawing) drawing).createChart(anchor);
//        XSSFChart chart = (XSSFChart) drawing.createChart(anchor);

        // 配置图表数据
        XDDFDataSource<String> categories = XDDFDataSourcesFactory.fromStringCellRange(
                sheet, new CellRangeAddress(dataRow, dataRow+1, 5, 5));
        XDDFNumericalDataSource<Double> values = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet, new CellRangeAddress(dataRow, dataRow+1, 6, 6));

        XDDFChartData data = chart.createData(ChartTypes.PIE, null, null);
//        XDDFChartData data = new XDDFPieChartData(chart.getCTChart());

        data.addSeries(categories, values);
        chart.plot(data);
        chart.setTitleText("总体评分分布");
    }

    private void createOverallChartData(XSSFSheet sheet, int rowNum, AiAnalysisBO analysis) {
        Row highRow = sheet.createRow(rowNum++);
        highRow.createCell(5).setCellValue("高分");
        highRow.createCell(6).setCellValue(analysis.getHighScoreEvaCount());

        Row otherRow = sheet.createRow(rowNum);
        otherRow.createCell(5).setCellValue("其他");
        otherRow.createCell(6).setCellValue(analysis.getTotalBeEvaCount() - analysis.getHighScoreEvaCount());
    }

    // 辅助方法
    private void addCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
        cell.setCellStyle(style);
    }

    private void addMergedCell(XSSFSheet sheet, Row row, int col, Object value,
                               CellStyle style, int firstCol, int lastCol) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value.toString());
        cell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), firstCol, lastCol));
    }

    // 样式创建方法
    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        font.setFontHeightInPoints((short)14);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createSubTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

}
