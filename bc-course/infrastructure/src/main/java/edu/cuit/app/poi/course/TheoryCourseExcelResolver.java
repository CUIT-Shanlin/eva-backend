package edu.cuit.app.poi.course;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cola.exception.BizException;
import edu.cuit.app.poi.course.util.ExcelCourseUtils;
import edu.cuit.app.poi.util.ExcelUtils;
import edu.cuit.client.bo.CourseExcelBO;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 理论课excel读取实现
 */
public class TheoryCourseExcelResolver extends CourseExcelResolverStrategy {

    /**
     * 节数段开始的行的集合
     * 上午1-2节从第4行开始
     */
    private final List<Integer> startRows = new ArrayList<>(List.of(5));
    private int endRow;

    // 星期开始列
    private final static Integer WEEK_START = 1;

    private Sheet sheet;

    protected TheoryCourseExcelResolver(InputStream excelFile) {
        this.excelFileStream = excelFile;
    }

    @Override
    public List<CourseExcelBO> readData() throws IOException, InvalidFormatException {
        sheet = WorkbookFactory.create(excelFileStream).getSheetAt(0);
        readStartRow();
        return read();
    }

    // 读取节数单元格开始的行数
    private void readStartRow() {
        int count;
        for (count = 0;count < 5;count++) {
            CellRangeAddress cra = ExcelUtils.getMergerCellRegionRow(sheet, startRows.get(count), 0);
            if (cra == null)
                throw new BizException("理论课程表格格式有误");
            startRows.add(cra.getLastRow() + 1);
        }
        CellRangeAddress cra = ExcelUtils.getMergerCellRegionRow(sheet, startRows.get(5), 0);
        if (cra == null)
            throw new BizException("理论课程表格格式有误");
        endRow = cra.getLastRow();
    }

    private List<CourseExcelBO> read() {

        Map<CourseExcelBO,List<CourseExcelBO>> courses = new HashMap<>();
        int rowCount = startRows.get(0);
        for (int i = rowCount; i <= endRow; i++) {
            List<CourseExcelBO> courseExcelBOS = readLine(i, getTime(i));

            for (CourseExcelBO courseExcelBO : courseExcelBOS) {
                courses.putIfAbsent(courseExcelBO,new ArrayList<>());
                courses.get(courseExcelBO).add(courseExcelBO);
            }

        }

        List<CourseExcelBO> result = new ArrayList<>();

        for (List<CourseExcelBO> sameCourse : courses.values()) {
            result.addAll(ExcelCourseUtils.mergeMultipleCourses(sameCourse));
        }

        return result;
    }

    /**
     * 读取一行，返回该行星期一到星期五的课程
     * @param rowIndex 行下标
     * @param startTime 开始节数
     * @return 键为星期数(从1开始)
     */
    private List<CourseExcelBO> readLine(int rowIndex,int startTime) {
        Row row = sheet.getRow(rowIndex);
        List<CourseExcelBO> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int startColumn = i * 9 + WEEK_START;
            Cell courseNameCell = row.getCell(startColumn);
            if (courseNameCell.getCellType() == CellType.BLANK) continue;
            String courseName = ExcelUtils.getCellStringValue(courseNameCell);
            if (StrUtil.isBlank(courseName)) continue;
            String teacherName = ExcelUtils.getCellStringValue(row.getCell(startColumn + 2));
            String profTitle = ExcelUtils.getCellStringValue(row.getCell(startColumn + 3));
            String weeksStr = ExcelUtils.getCellStringValue(row.getCell(startColumn + 5));
            String classroom = ExcelUtils.getCellStringValue(row.getCell(startColumn + 6));
            String courseClass = ExcelUtils.getCellStringValue(row.getCell(startColumn + 7));

            CourseExcelBO courseExcelBO = new CourseExcelBO();
            courseExcelBO
                    .setCourseName(courseName)
                    .setTeacherName(teacherName)
                    .setProfTitle(profTitle)
                    .setWeeks(ExcelUtils.resolveWeekString(weeksStr))
                    .setClassroom(classroom)
                    .setCourseClass(courseClass)
                    .setDay(i + 1);
            courseExcelBO.setStartTime(startTime);
            if (startTime == 11) {
                courseExcelBO.setEndTime(11);
            } else courseExcelBO.setEndTime(startTime + 1);
            results.add(courseExcelBO);
        }
        return results;
    }

    /**
     * 读取某行数所在的节数
     * @param rowIndex 行数下标
     * @return 开始节数下标
     * 1代表上午1-2节，3代表上午3-4节，5代表下午5-6节以此类推
     */
    private Integer getTime(int rowIndex) {
        for (int i = 0; i < startRows.size() - 1; i++) {
            if (startRows.get(i) <= rowIndex && startRows.get(i+1) > rowIndex) {
                return i * 2 + 1;
            }
        }
        return 11;
    }

}
