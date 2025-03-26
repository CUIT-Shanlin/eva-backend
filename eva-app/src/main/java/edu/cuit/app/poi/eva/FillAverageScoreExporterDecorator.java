package edu.cuit.app.poi.eva;

import edu.cuit.app.poi.util.ExcelUtils;
import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.eva.CourseScoreCO;
import edu.cuit.client.dto.clientobject.eva.UserSingleCourseScoreCO;
import edu.cuit.domain.entity.user.biz.UserEntity;
import org.apache.poi.ss.usermodel.Row;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 填充课程分数sheet
 */
public class FillAverageScoreExporterDecorator extends EvaStatisticsExporter {

    private int rowIndex = 2;

    protected FillAverageScoreExporterDecorator(EvaStatisticsExporter exporter) {
        super(exporter.semId);
        this.exporter = exporter;
    }

    @Override
    protected void process() {
        exporter.process();
        workbook = exporter.getWorkbook();
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
            createCell(courseFirstRow,4).setCellValue(getCourseNature(course));
            createCell(courseFirstRow,5).setCellValue(userSingleCourseScore.getEvaNum());
            createCell(courseFirstRow,12).setCellValue(userSingleCourseScore.getScore() < 0 ? "-" : userSingleCourseScore.getScore().toString());

            // 处理课程指标
            List<CourseScoreCO> courseScoreList = courseDetailService.evaResult(course.getCourseBaseMsg().getId());
            if (!courseScoreList.isEmpty()) {
                for (CourseScoreCO courseScore : courseScoreList) {
                    Row propsRow = sheet.getRow(rowIndex);
                    if (propsRow == null) propsRow = sheet.createRow(rowIndex);
                    propsRow.setHeight((short)(24*20));

                    ExcelUtils.createRegion(rowIndex, rowIndex,6,8,sheet);

                    String propStr = courseScore.getProp();
                    int delimiterIndex = propStr.indexOf('|');
                    String maxScore = propStr.substring(0, delimiterIndex);
                    propStr = propStr.substring(delimiterIndex + 1);

                    createCell(propsRow,6).setCellValue(String.format("%s (%s)",propStr,maxScore));

                    int textLength = 0;
                    try {
                        textLength = (propStr.getBytes("GBK").length) * 256;
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                    int columnWidth = sheet.getColumnWidth(6);
                    if (textLength > columnWidth * 3) {
                        propsRow.setHeight((short) (((textLength / (columnWidth*3)) + 4) * (propsRow.getHeight() / 2)));
                    }

                    createCell(propsRow,9).setCellValue(courseScore.getMinScore() <= -1 ? "-" : courseScore.getMinScore().toString());
                    createCell(propsRow,10).setCellValue(courseScore.getAverScore() <= -1 ? "-" : courseScore.getAverScore().toString());
                    createCell(propsRow,11).setCellValue(courseScore.getMaxScore() <= -1 ? "-" : courseScore.getMaxScore().toString());
                    rowIndex++;
                }
            } else {
                Row propsRow = sheet.getRow(rowIndex);
                if (propsRow == null) propsRow = sheet.createRow(rowIndex);
                propsRow.setHeight((short)(24*20));

                ExcelUtils.createRegion(rowIndex, rowIndex,6,8 ,sheet);
                createCell(propsRow,6).setCellValue("无指标");
                createCell(propsRow,9).setCellValue("-");
                createCell(propsRow,10).setCellValue("-");
                createCell(propsRow,11).setCellValue("-");
                rowIndex++;
            }

            // 合并单元格
            ExcelUtils.createRegion(courseStartRowIndex, rowIndex - 1,2,3,sheet);
            ExcelUtils.createRegion(courseStartRowIndex, rowIndex - 1,4,4,sheet);
            ExcelUtils.createRegion(courseStartRowIndex, rowIndex - 1,5,5,sheet);
            ExcelUtils.createRegion(courseStartRowIndex, rowIndex - 1, 12, 13,sheet);

        }

        ExcelUtils.createRegion(teacherStartRowIndex, rowIndex - 1, 0, 1,sheet);

    }

    private void createHeader() {
        addTitle("教师评教分数统计",13);

        Row headerRow = sheet.createRow(1);
        headerRow.setHeight((short)(24*25));


        // 处理教师格
        createHeaderCell(0,1,"教师",headerRow);
        createHeaderCell(2,3,"课程",headerRow);
        createHeaderCell(4,4,"性质",headerRow);
        createHeaderCell(5,5,"评教次数",headerRow);
        createHeaderCell(6,8,"指标 (满分)",headerRow);
        createHeaderCell(9,9,"最低分",headerRow);
        createHeaderCell(10,10,"平均分",headerRow);
        createHeaderCell(11,11,"最高分",headerRow);
        createHeaderCell(12,13,"课程平均分",headerRow);

    }

}
