package edu.cuit.app.poi.eva;

import com.alibaba.cola.exception.SysException;
import edu.cuit.app.poi.util.ExcelUtils;
import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.eva.UserSingleCourseScoreCO;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 填充课程评教数据sheet
 */
@Slf4j
public class FillEvaRecordExporterDecorator extends EvaStatisticsExporter{

    public FillEvaRecordExporterDecorator(EvaStatisticsExporter exporter) {
        super(exporter.semId);
        this.exporter = exporter;
    }

    @Override
    protected void process() {
        exporter.process();
        workbook = exporter.getWorkbook();
        sheet = workbook.createSheet("评价详细");

        createHeader();
        insertData();
    }

    private int rowIndex = 2;

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

        Map<Integer, UserSingleCourseScoreCO> userScoreMap = new HashMap<>();
        userService.getOneUserScore(teacher.getId(),semId).forEach(scoreInfo -> {
            userScoreMap.put(scoreInfo.getCourseId(),scoreInfo);
        });

        int teacherStartRowIndex = rowIndex;


        for (CourseDetailCO course : courseList) {
            int courseStartRowIndex = rowIndex;

            // 处理课程平均分
            UserSingleCourseScoreCO userSingleCourseScore = userScoreMap.get(course.getCourseBaseMsg().getId());
            Row courseFirstRow = getOrCreateRow(rowIndex);


            List<EvaRecordEntity> courseRecordList = evaQueryGateway.getRecordByCourse(course.getCourseBaseMsg().getId());

            if (courseRecordList.isEmpty()) continue;

            createCell(courseFirstRow,5)
                    .setCellValue(userSingleCourseScore.getScore());
            createCell(courseFirstRow,2).setCellValue(userSingleCourseScore.getCourseName());
            createCell(courseFirstRow,4).setCellValue(getCourseNature(course));

            for (EvaRecordEntity courseRecord : courseRecordList) {
                Row propsRow = getOrCreateRow(rowIndex);

                // 设置评价内容
                String text = courseRecord.getTextValue();
                Cell textCell = createCell(propsRow, 7);
                textCell.setCellValue(text);

                XSSFCellStyle textCellStyle = workbook.createCellStyle();
                textCellStyle.cloneStyleFrom(getContentStyle());
                textCellStyle.setAlignment(HorizontalAlignment.LEFT);
                textCell.setCellStyle(textCellStyle);

                int textLength = 0;
                try {
                    textLength = (text.getBytes("GBK").length) * 256;
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                int columnWidth = sheet.getColumnWidth(6);
                if (textLength > columnWidth * 4) {
                    propsRow.setHeight((short) (((textLength / (columnWidth*4)) + 1) * (propsRow.getHeight() / 2)));
                }

                Double score = evaQueryGateway.getScoreByProp(courseRecord.getFormPropsValues()).orElseThrow(() -> {
                    SysException e = new SysException("导出失败，请联系管理员");
                    log.error("发生系统异常", e);
                    return e;
                });
                if (score < 0) {
                    createCell(propsRow,6).setCellValue("无指标分数");
                } else createCell(propsRow,6).setCellValue(new DecimalFormat("#.00").format(score));

                ExcelUtils.createRegion(rowIndex,rowIndex,7,10,sheet);

                rowIndex++;
            }

            ExcelUtils.createRegion(courseStartRowIndex,rowIndex - 1,2,3,sheet);
            ExcelUtils.createRegion(courseStartRowIndex,rowIndex - 1,4,4,sheet);
            ExcelUtils.createRegion(courseStartRowIndex,rowIndex - 1,5,5,sheet);
        }

        if (teacherStartRowIndex == rowIndex) return;
        createCell(getOrCreateRow(teacherStartRowIndex),0).setCellValue(teacher.getName());

        ExcelUtils.createRegion(teacherStartRowIndex,rowIndex - 1,0,1,sheet);
    }

    private void createHeader() {
        addTitle("教师评教评价统计",10);

        Row headerRow = sheet.createRow(1);
        headerRow.setHeight((short)(24*25));

        createHeaderCell(0,1,"教师",headerRow);
        createHeaderCell(2,3,"课程",headerRow);
        createHeaderCell(4,4,"性质",headerRow);
        createHeaderCell(5,5,"课程平均分",headerRow);
        createHeaderCell(6,6,"分数",headerRow);
        createHeaderCell(7,10,"评价信息",headerRow);
    }

    private Row getOrCreateRow(int rowIndex) {
        XSSFRow row = (XSSFRow) sheet.getRow(rowIndex);
        if (row == null) {
            row = (XSSFRow) sheet.createRow(rowIndex);
            row.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());
        }
        return row;
    }

}
