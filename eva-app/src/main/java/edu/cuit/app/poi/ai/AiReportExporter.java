package edu.cuit.app.poi.ai;

import com.alibaba.cola.exception.SysException;
import edu.cuit.client.bo.ai.AiAnalysisBO;
import edu.cuit.client.bo.ai.AiCourseSuggestionBO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.util.Units;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

@Slf4j
public class AiReportExporter {

    private static final int CHART_WIDTH = 400;
    private static final int CHART_HEIGHT = 300;
    private static final int PAGE_WIDTH_TWIPS = 12240; // A4纸宽度(24cm)对应的Twips值

    public XWPFDocument generateReport(AiAnalysisBO analysis) {
        XWPFDocument doc = new XWPFDocument();
        setGlobalStyles(doc);
        createCoverPage(doc, analysis.getTeacherName());
        createCourseAnalysisSection(doc, analysis);
        createOverallAnalysisSection(doc, analysis);
        return doc;
    }

    private void setGlobalStyles(XWPFDocument doc) {
        CTFonts ctFonts = CTFonts.Factory.newInstance();
        ctFonts.setEastAsia("微软雅黑");
        doc.createStyles().setDefaultFonts(ctFonts);
    }

    private void createCoverPage(XWPFDocument doc, String teacherName) {
        XWPFParagraph titlePara = doc.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText("教师教学质量评估报告");
        titleRun.setFontSize(28);
        titleRun.setBold(true);
        titleRun.setColor("2E75B5");

        addEmptyLines(titlePara, 2);

        XWPFParagraph teacherPara = doc.createParagraph();
        teacherPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun teacherRun = teacherPara.createRun();
        teacherRun.setText("被评估教师：" + teacherName);
        teacherRun.setFontSize(22);
        teacherRun.setColor("5B9BD5");
    }

    private void createCourseAnalysisSection(XWPFDocument doc, AiAnalysisBO analysis) {
        addSectionTitle(doc, "课程专项分析");

        for (AiCourseSuggestionBO course : analysis.getCourseSuggestions()) {
            createCourseTable(doc, course);
            addChartToDoc(doc, createCourseChart(course));
            addEmptyLines(doc.createParagraph(), 2);
        }
    }

    private void createCourseTable(XWPFDocument doc, AiCourseSuggestionBO course) {
        XWPFTable table = doc.createTable(4, 2);
//        configureTableLayout(table, new int[]{30, 70}); // 调整为3:7的列宽比例

        fillTableHeader(table.getRow(0), "课程名称", course.getCourseName());
        fillTableRow(table.getRow(1),
                "评教统计",
                formatEvaStats(course.getBeEvaNumCount(), course.getHighScoreBeEvaCount()));
        fillMultiLineCell(table.getRow(2), "课程分析",
                formatAnalysis(course.getAdventures(), course.getDrawbacks()));
        fillMultiLineCell(table.getRow(3), "改进建议", course.getSuggestion());
    }

    private void createOverallAnalysisSection(XWPFDocument doc, AiAnalysisBO analysis) {
        addSectionTitle(doc, "总体评估报告");

        XWPFTable overallTable = doc.createTable(2, 2);
//        configureTableLayout(overallTable, new int[]{30, 40});
        overallTable.setWidth("100%");

        fillTableRow(overallTable.getRow(0), "总评次数", analysis.getTotalBeEvaCount().toString());
        fillTableRow(overallTable.getRow(1), "高分次数", analysis.getHighScoreEvaCount().toString());

        addAnalysisParagraph(doc, analysis.getOverallReport());
        addChartToDoc(doc, createOverallChart(analysis));
    }

    // 表格布局优化方法
    private void configureTableLayout(XWPFTable table, int[] columnPercentages) {
        // 设置表格整体宽度为页面宽度
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        CTTblWidth tblWidth = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
        tblWidth.setType(STTblWidth.DXA);
        tblWidth.setW(BigInteger.valueOf(PAGE_WIDTH_TWIPS));

        // 设置列宽比例
        for (int i = 0; i < columnPercentages.length; i++) {
            int columnWidth = (int) (PAGE_WIDTH_TWIPS * columnPercentages[i] / 100.0);
            CTTblWidth ctTblWidth = table.getRow(0).getCell(i).getCTTc().addNewTcPr().addNewTcW();
            ctTblWidth.setType(STTblWidth.DXA);
//            ctTblWidth.setW(BigInteger.valueOf(columnWidth));
        }
    }

    // 带格式的评教统计
    private String formatEvaStats(int total, int highScore) {
        return String.format("总评次数：%-6d | 高分次数：%-4d (%.1f%%)",
                total, highScore, (highScore * 100.0 / total));
    }

    // 带格式的课程分析
    private String formatAnalysis(String advantages, String disadvantages) {
        return String.format("优点：\\n%s\\n\\n缺点：\\n%s", advantages, disadvantages);
    }

    // 带换行符处理的段落生成
    private void addAnalysisParagraph(XWPFDocument doc, String text) {
        XWPFParagraph para = doc.createParagraph();
        para.setIndentationFirstLine(600);
        XWPFRun run = para.createRun();
        run.setText("AI总体评价：");
        run.setBold(true);
        run.setColor("FF0000");
        addFormattedText(run, text);
    }

    // 核心改进：带换行符的文本处理方法
    private void addFormattedText(XWPFRun run, String text) {
        String[] lines = text.split("\\\\n");
        for (int i = 0; i < lines.length; i++) {
            run.setText(lines[i]);
            if (i > 0) {
                run.addBreak(); // 换行符转换为Word换行
                run.addCarriageReturn();
            }
        }
    }

    // 表格单元格处理方法（支持自动换行）
    private void fillMultiLineCell(XWPFTableRow row, String label, String content) {
        styleCell(row.getCell(0), label, "D9E1F2", true);
        XWPFTableCell contentCell = row.getCell(1);
        contentCell.removeParagraph(0);
        XWPFParagraph para = contentCell.addParagraph();
        para.setSpacingBetween(1.5);
        XWPFRun run = para.createRun();
        addFormattedText(run, content);
        run.setFontSize(12);
    }

    // 其他保持不变的图表相关方法
    private JFreeChart createCourseChart(AiCourseSuggestionBO course) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("95+", course.getHighScoreBeEvaCount());
        dataset.setValue("95-", course.getBeEvaNumCount() - course.getHighScoreBeEvaCount());

        JFreeChart chart = ChartFactory.createPieChart(
                course.getCourseName() + "评分分布",
                dataset,
                true, true, false
        );
        styleChart(chart);
        return chart;
    }

    private JFreeChart createOverallChart(AiAnalysisBO analysis) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("95+", analysis.getHighScoreEvaCount());
        dataset.setValue("95-", analysis.getTotalBeEvaCount() - analysis.getHighScoreEvaCount());

        JFreeChart chart = ChartFactory.createPieChart(
                "总体评分分布",
                dataset,
                true, true, false
        );
        styleChart(chart);
        return chart;
    }

    private void styleChart(JFreeChart chart) {
        chart.getTitle().setFont(new java.awt.Font("微软雅黑", java.awt.Font.BOLD, 16));
        chart.getLegend().setItemFont(new java.awt.Font("微软雅黑", java.awt.Font.PLAIN, 12));
    }

    private void addChartToDoc(XWPFDocument doc, JFreeChart chart) {
        try (ByteArrayOutputStream chartOut = new ByteArrayOutputStream()) {
            ChartUtils.writeChartAsPNG(chartOut, chart, CHART_WIDTH, CHART_HEIGHT);
            XWPFParagraph chartPara = doc.createParagraph();
            chartPara.setAlignment(ParagraphAlignment.CENTER);

            chartPara.createRun().addPicture(
                    new ByteArrayInputStream(chartOut.toByteArray()),
                    XWPFDocument.PICTURE_TYPE_PNG,
                    "chart.png",
                    Units.toEMU(CHART_WIDTH),
                    Units.toEMU(CHART_HEIGHT)
            );
        } catch (IOException | InvalidFormatException e) {
            log.error("图表生成失败", e);
            throw new SysException("图表生成失败");
        }
    }

    // 其他辅助方法保持不变
    private void addSectionTitle(XWPFDocument doc, String title) {
        XWPFParagraph sectionTitle = doc.createParagraph();
        sectionTitle.setStyle("Heading1");
        sectionTitle.createRun().setText(title);
        sectionTitle.setAlignment(ParagraphAlignment.CENTER);
    }

    private void fillTableHeader(XWPFTableRow row, String title, String content) {
        styleCell(row.getCell(0), title, "2E75B5", true);
        styleCell(row.getCell(1), content, "FFFFFF", false);
    }

    private void fillTableRow(XWPFTableRow row, String label, String value) {
        styleCell(row.getCell(0), label, "D9E1F2", false);
        styleCell(row.getCell(1), value, "FFFFFF", false);
    }

    private void styleCell(XWPFTableCell cell, String text, String bgColor, boolean isHeader) {
        cell.removeParagraph(0);
        XWPFParagraph para = cell.addParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun run = para.createRun();
        run.setText(text);

        run.setFontSize(isHeader ? 14 : 12);
        run.setBold(isHeader);
        run.setColor(isHeader ? "FFFFFF" : "000000");

        CTShd cTShd = cell.getCTTc().addNewTcPr().addNewShd();
        cTShd.setFill(bgColor);
    }

    /*private void styleCell(XWPFTableCell cell, String text, String bgColor, boolean isHeader) {
        cell.removeParagraph(0);
        XWPFParagraph para = cell.addParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);

        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            XWPFRun run = para.createRun();
            run.setText(lines[i]);
            run.setFontSize(isHeader ? 14 : 12);
            run.setBold(isHeader);
            run.setColor(isHeader ? "FFFFFF" : "000000");

            // 在非最后一行末尾添加换行符
            if (i < lines.length - 1) {
                run.addBreak();
                run.addCarriageReturn();
            }
        }

        CTShd cTShd = cell.getCTTc().addNewTcPr().addNewShd();
        cTShd.setFill(bgColor);
    }*/

    private void addEmptyLines(XWPFParagraph para, int lines) {
        for (int i = 0; i < lines; i++) {
            para.createRun().addBreak();
        }
    }
}