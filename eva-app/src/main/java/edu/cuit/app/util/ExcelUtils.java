package edu.cuit.app.util;

import cn.hutool.core.util.StrUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.ArrayList;
import java.util.List;

/**
 * 表格解析工具类
 */
public class ExcelUtils {

    /**
     * 获取某行某列的合并单元格
     * @param sheet 表格sheet
     * @param cellRow 起始行
     * @param cellCol 起始列
     * @return 左上角单元格
     */
    public static CellRangeAddress getMergerCellRegionRow(Sheet sheet, int cellRow, int cellCol) {
        // 获取sheet工作表中所有的合并单元格的个数
        int sheetMergerCount = sheet.getNumMergedRegions();
        for (int i = 0; i < sheetMergerCount; i++) {
            // 获取指定合并单元格
            CellRangeAddress cra = sheet.getMergedRegion(i);
            int firstRow = cra.getFirstRow(); // 合并单元格CELL起始行
            int firstCol = cra.getFirstColumn(); // 合并单元格CELL起始列
            if (cellRow == firstRow && cellCol == firstCol) {
                return cra;
            }
        }
        return null;
    }

    /**
     * 获取合并单元格的第一个单元格
     * @param sheet 表格sheet
     * @param cra 合并单元格
     * @return 单元格
     */
    public static Cell getMergedCellRegionFistCell(Sheet sheet,CellRangeAddress cra) {
        return sheet.getRow(cra.getFirstRow()).getCell(cra.getFirstColumn());
    }

    /**
     * 解析周数字符串，返回周数
     * 以逗号分隔，-表示连续周，结尾为" 周"
     * @param weeksStr 周数字符串
     * @return 周数集合
     */
    public static List<Integer> resolveWeekString(String weeksStr) {
        String[] str = StrUtil.subPre(weeksStr, weeksStr.length() - 2).split(",");
        List<Integer> result = new ArrayList<>();
        for (String s : str) {
            String[] value = s.split("-");
            if (value.length == 1) {
                result.add(Integer.parseInt(value[0]));
            } else {
                int start = Integer.parseInt(value[0]);
                int end = Integer.parseInt(value[1]);
                for (int i = start; i <= end; i++) {
                    result.add(i);
                }
            }
        }
        return result;
    }

}
