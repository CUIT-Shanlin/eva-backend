package edu.cuit.app.poi.eva;

import cn.hutool.extra.spring.SpringUtil;
import edu.cuit.app.poi.util.ExcelUtils;
import edu.cuit.client.api.ISemesterService;
import edu.cuit.client.api.course.ICourseDetailService;
import edu.cuit.client.api.course.ICourseService;
import edu.cuit.client.api.course.IUserCourseService;
import edu.cuit.client.api.user.IUserService;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.eva.CourseScoreCO;
import edu.cuit.client.dto.clientobject.eva.UserSingleCourseScoreCO;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 填充课程分数sheet
 */
public class FillAverageScoreExporterDecorator extends EvaStatisticsExporter {

    private static final UserQueryGateway userQueryGateway;
    private static final ICourseDetailService courseDetailService;
    private static final IUserCourseService userCourseService;
    private static final IUserService userService;
    private static final ISemesterService semesterService;

    static {
        userQueryGateway = SpringUtil.getBean(UserQueryGateway.class);
        courseDetailService = SpringUtil.getBean(ICourseDetailService.class);
        userCourseService = SpringUtil.getBean(IUserCourseService.class);
        userService = SpringUtil.getBean(IUserService.class);
        semesterService = SpringUtil.getBean(ISemesterService.class);
    }

    private final EvaStatisticsExporter exporter;

    private Sheet sheet;

    private int rowIndex = 2;

    protected FillAverageScoreExporterDecorator(EvaStatisticsExporter exporter) {
        this.exporter = exporter;
        semId = exporter.semId;
    }

    @Override
    protected void process() {
        exporter.process();
        sheet = workbook.createSheet("分数详情");

        createHeader();
        insertData();
    }

    private void insertData() {
        List<UserEntity> teacherList = userQueryGateway.findAllUserId().stream()
                .map(userId -> userQueryGateway.findById(userId).orElse(null))
                .filter(user -> user != null && !"admin".equals(user.getUsername()))
                .toList();
        for (UserEntity teacher : teacherList) {
            insertTeacherData(teacher);
        }
    }

    private void insertTeacherData(UserEntity teacher) {
        List<CourseDetailCO> courseList = userCourseService.getUserCourses(semId, teacher.getId()).stream()
                .map(courseId -> courseDetailService.courseInfo(courseId, semId))
                .toList();

        if (courseList.isEmpty()) return;

        Map<Integer,UserSingleCourseScoreCO> userScoreMap = new HashMap<>();
        userService.getOneUserScore(teacher.getId(),semId).forEach(scoreInfo -> {
            userScoreMap.put(scoreInfo.getCourseId(),scoreInfo);
        });

        int teacherStartRowIndex = rowIndex;

        createCell(sheet.createRow(teacherStartRowIndex),0).setCellValue(teacher.getName());

        for (CourseDetailCO course : courseList) {
            int courseStartRowIndex = rowIndex;

            // 处理评教人数和课程平均分
            UserSingleCourseScoreCO userSingleCourseScore = userScoreMap.get(course.getCourseBaseMsg().getId());
            Row courseFirstRow = sheet.getRow(rowIndex);
            if (courseFirstRow == null) courseFirstRow = sheet.createRow(rowIndex);
            createCell(courseFirstRow,2).setCellValue(userSingleCourseScore.getCourseName());
            createCell(courseFirstRow,4).setCellValue(userSingleCourseScore.getEvaNum());
            createCell(courseFirstRow,11).setCellValue(userSingleCourseScore.getScore());

            // 处理课程指标
            List<CourseScoreCO> courseScoreList = courseDetailService.evaResult(course.getCourseBaseMsg().getId());
            if (!courseScoreList.isEmpty()) {
                for (CourseScoreCO courseScore : courseScoreList) {
                    Row propsRow = sheet.getRow(rowIndex);
                    if (propsRow == null) propsRow = sheet.createRow(rowIndex);
                    propsRow.setHeight((short)(24*20));

                    ExcelUtils.createRegion(rowIndex, rowIndex,5,7,sheet);
                    createCell(propsRow,5).setCellValue(courseScore.getProp());
                    createCell(propsRow,8).setCellValue(courseScore.getMinScore());
                    createCell(propsRow,9).setCellValue(courseScore.getAverScore());
                    createCell(propsRow,10).setCellValue(courseScore.getMaxScore());
                    rowIndex++;
                }
            } else {
                Row propsRow = sheet.getRow(rowIndex);
                if (propsRow == null) propsRow = sheet.createRow(rowIndex);
                propsRow.setHeight((short)(24*20));

                ExcelUtils.createRegion(rowIndex, rowIndex,5,7 ,sheet);
                createCell(propsRow,5).setCellValue("无指标");
                createCell(propsRow,8).setCellValue(-1);
                createCell(propsRow,9).setCellValue(-1);
                createCell(propsRow,10).setCellValue(-1);
                rowIndex++;
            }

            // 合并单元格
            ExcelUtils.createRegion(courseStartRowIndex, rowIndex - 1,2,3,sheet);
            ExcelUtils.createRegion(courseStartRowIndex, rowIndex - 1,4,4,sheet);
            ExcelUtils.createRegion(courseStartRowIndex, rowIndex - 1, 11, 12,sheet);

        }

        ExcelUtils.createRegion(teacherStartRowIndex, rowIndex - 1, 0, 1,sheet);

    }

    private Cell createCell(Row row,int col) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(contentStyle);
        return cell;
    }

    private void createHeader() {
        Row titleRow = sheet.createRow(0);
        titleRow.setHeight((short)(24*25));
        ExcelUtils.createRegion(0,0,0,12,sheet);
        Cell cell = createCell(titleRow, 0);
        SemesterCO semester = semesterService.semesterInfo(semId);
        StringBuilder titleText = new StringBuilder()
                .append(semester.getStartYear()).append("-")
                .append(semester.getEndYear())
                .append("学年第")
                .append(semester.getPeriod() + 1).append("学期")
                .append("教师评教分数统计")
                .append("(导出时间：")
                .append(LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .append(" )");
        cell.setCellValue(titleText.toString());

        Row headerRow = sheet.createRow(1);
        headerRow.setHeight((short)(24*25));


        // 处理教师格
        createHeaderCell(0,1,"教师",headerRow);
        createHeaderCell(2,3,"课程",headerRow);
        createHeaderCell(4,4,"评教次数",headerRow);
        createHeaderCell(5,7,"指标",headerRow);
        createHeaderCell(8,8,"最低分",headerRow);
        createHeaderCell(9,9,"平均分",headerRow);
        createHeaderCell(10,10,"最高分",headerRow);
        createHeaderCell(11,12,"课程平均分",headerRow);

    }

    private void createHeaderCell(int firstCol,int lastCol,String content,Row row) {
        if (firstCol != lastCol) {
            ExcelUtils.createRegion(1, 1, firstCol, lastCol,sheet);
        }
        Cell cell = row.createCell(firstCol);
        cell.setCellValue(content);
        cell.setCellStyle(headerStyle);
    }

}
