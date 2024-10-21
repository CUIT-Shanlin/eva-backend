package edu.cuit.app.resolver.course;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cola.exception.BizException;
import com.alibaba.cola.exception.SysException;
import edu.cuit.app.resolver.course.util.ExcelUtils;
import edu.cuit.client.bo.CourseExcelBO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实验课excel读取实现
 */
@Slf4j
public class ExperimentalCourseResolver extends CourseExcelResolverStrategy{

    private Sheet sheet;

    // 每个课程时间段的开始行下标和结束行下标
    private final List<Pair<Integer,Integer>> timeTable = new ArrayList<>();

    // 每个星期天数的开始列下表
    private final List<Integer> dayTable = new ArrayList<>(List.of(1));
    private int dayEndCol;

    // 第一节课的开始行下标
    private final static Integer TIME_START_ROW = 3;

    protected ExperimentalCourseResolver(InputStream excelFileStream) {
        this.excelFileStream = excelFileStream;
    }

    @Override
    public List<CourseExcelBO> readData() throws IOException, InvalidFormatException {
        sheet = WorkbookFactory.create(excelFileStream).getSheetAt(0);
        readTimeTable();
        readDayTable();
        return read();
    }

    private List<CourseExcelBO> read() {
        Map<CourseExcelBO,CourseExcelBO> results = new HashMap<>();
        for (int x = 0; x < timeTable.size(); x++) {
            Pair<Integer, Integer> timePeriod = timeTable.get(x);
            for (int i = timePeriod.getKey(); i <= timePeriod.getValue(); i++) {
                List<CourseExcelBO> lineResults = readLine(i,x * 2 + 1);
                for (CourseExcelBO lineResult : lineResults) {
                    CourseExcelBO existedCourse = results.get(lineResult);
                    if (existedCourse != null && lineResult.isAdjoin(existedCourse)) {
                        ExcelUtils.mergeTwoCourse(existedCourse,lineResult);
                    }
                    results.put(lineResult,lineResult);
                }
            }
        }
        return new ArrayList<>(results.values());
    }

    private List<CourseExcelBO> readLine(int rowIndex,int startTime) {
        Row row = sheet.getRow(rowIndex);
        List<CourseExcelBO> results = new ArrayList<>();
        Row classRoomRow = sheet.getRow(1);
        for (int dayIndex = 0; dayIndex < dayTable.size(); dayIndex++) {
            int startCol = dayTable.get(dayIndex);
            List<CourseExcelBO> oneDayCourses = new ArrayList<>();
            for (int i = startCol;i < (dayIndex == dayTable.size() - 1 ? dayEndCol : dayTable.get(dayIndex + 1)) ; i += 4) {
                Cell weeksCell = row.getCell(i);
                if (weeksCell.getCellType() == CellType.BLANK) continue;
                String weeks = ExcelUtils.getCellStringValue(weeksCell);
                if (StrUtil.isBlank(weeks)) continue;
                String teacherName = ExcelUtils.getCellStringValue(row.getCell(i + 1));
                String courseClass = ExcelUtils.getCellStringValue(row.getCell(i + 2));
                String courseName = ExcelUtils.getCellStringValue(row.getCell(i + 3));
                String classroom = ExcelUtils.getCellStringValue(classRoomRow.getCell(startCol));
                CourseExcelBO courseExcelBO = new CourseExcelBO()
                        .setTeacherName(teacherName)
                        .setCourseClass(courseClass)
                        .setCourseName(courseName)
                        .setClassroom(classroom)
                        .setDay(dayIndex + 1)
                        .setStartTime(startTime)
                        .setEndTime(startTime + 1)
                        .setWeeks(ExcelUtils.resolveWeekString(weeks));
                oneDayCourses.add(courseExcelBO);
            }
            results.addAll(oneDayCourses);
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
        for (int i = 0; i < timeTable.size(); i++) {
            Pair<Integer, Integer> period = timeTable.get(i);
            if (period.getKey() <= rowIndex && period.getValue() >= rowIndex) {
                return i * 2 + 1;
            }
        }
        return 11;
    }

    private void readTimeTable() {
        int count;
        int tmpStartRow = TIME_START_ROW;
        for (count = 0;count < 6;count++) {
            CellRangeAddress cra = ExcelUtils.getMergerCellRegionRow(sheet, tmpStartRow, 0);
            if (cra == null)
                throw new BizException("理论课程表格格式有误");
            timeTable.add(Pair.of(tmpStartRow,cra.getLastRow()));
            CellRangeAddress blankCra = ExcelUtils.getMergerCellRegionRow(sheet, cra.getLastRow() + 1, 0);

            if (count == 5) break;
            if (blankCra == null) {
                SysException e = new SysException("解析文件出错，请联系管理员");
                log.error("发生系统异常",e);
                throw e;
            }
            if (blankCra.getLastColumn() > 1) {
                tmpStartRow = blankCra.getLastRow() + 1;
            } else tmpStartRow = cra.getLastRow() + 1;
        }
    }

    private void readDayTable() {
        int count;
        for (count = 0;count < 6;count++) {
            CellRangeAddress cra = ExcelUtils.getMergerCellRegionRow(sheet, 0, dayTable.get(count));
            if (cra == null) throw new BizException("理论课程表格格式有误");
            dayTable.add(cra.getLastColumn() + 1);
        }
        CellRangeAddress cra = ExcelUtils.getMergerCellRegionRow(sheet, 0, dayTable.get(6));
        if (cra == null)
            throw new BizException("实验课程表格格式有误");
        dayEndCol = cra.getLastRow();
    }

}
