package edu.cuit.app.poi.eva;

import cn.hutool.extra.spring.SpringUtil;
import edu.cuit.app.poi.util.ExcelUtils;
import edu.cuit.client.api.ISemesterService;
import edu.cuit.client.api.course.ICourseDetailService;
import edu.cuit.client.api.course.IUserCourseService;
import edu.cuit.client.api.user.IUserService;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.bc.evaluation.application.port.EvaStatisticsQueryPort;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import lombok.Getter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EvaStatisticsExporter {

    protected static final ISemesterService semesterService;
    protected static final UserQueryGateway userQueryGateway;
    protected static final ICourseDetailService courseDetailService;
    protected static final IUserCourseService userCourseService;
    protected static final IUserService userService;
    protected static final EvaStatisticsQueryPort evaStatisticsQueryPort;

    static {
        semesterService = SpringUtil.getBean(ISemesterService.class);
        userQueryGateway = SpringUtil.getBean(UserQueryGateway.class);
        courseDetailService = SpringUtil.getBean(ICourseDetailService.class);
        userCourseService = SpringUtil.getBean(IUserCourseService.class);
        userService = SpringUtil.getBean(IUserService.class);
        evaStatisticsQueryPort = SpringUtil.getBean(EvaStatisticsQueryPort.class);
    }

    @Getter
    protected XSSFWorkbook workbook;

    protected EvaStatisticsExporter exporter = null;

    protected CellStyle headerStyle;
    protected CellStyle contentStyle;

    protected Sheet sheet;

    protected int semId;

    public EvaStatisticsExporter(int semId) {
        this.semId = semId;
    }

    protected void process() {
        workbook = new XSSFWorkbook();
        headerStyle = ExcelUtils.getHeaderStyle(workbook);
        contentStyle = ExcelUtils.getContentStyle(workbook);
    }

    protected void addTitle(String title,int width) {
        Row titleRow = sheet.createRow(0);
        titleRow.setHeight((short)(24*25));
        ExcelUtils.createRegion(0,0,0,width,sheet);
        Cell cell = createCell(titleRow, 0);
        SemesterCO semester = semesterService.semesterInfo(semId);
        StringBuilder titleText = new StringBuilder()
                .append(semester.getStartYear()).append("-")
                .append(semester.getEndYear())
                .append("学年第")
                .append(semester.getPeriod() + 1).append("学期")
                .append(title)
                .append("(导出时间：")
                .append(LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .append(" )");
        cell.setCellValue(titleText.toString());
    }

    protected String getCourseNature(CourseDetailCO course) {
        return course.getCourseBaseMsg().getNature() == 0 ? "理论课" : "实验课";
    }

    protected CellStyle getHeaderStyle() {
        if (exporter == null) return headerStyle;
        return exporter.getHeaderStyle();
    }

    protected CellStyle getContentStyle() {
        if (exporter == null) return contentStyle;
        return exporter.getContentStyle();
    }

    protected Cell createCell(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) cell = row.createCell(col);
        cell.setCellStyle(getContentStyle());
        return cell;
    }

    protected void createHeaderCell(int firstCol, int lastCol, String content, Row row) {
        if (firstCol != lastCol) {
            ExcelUtils.createRegion(1, 1, firstCol, lastCol,sheet);
        }
        Cell cell = row.createCell(firstCol);
        cell.setCellValue(content);
        cell.setCellStyle(getHeaderStyle());
    }

}
