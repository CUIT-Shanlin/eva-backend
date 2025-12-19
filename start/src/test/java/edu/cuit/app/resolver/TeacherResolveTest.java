package edu.cuit.app.resolver;

import edu.cuit.app.poi.util.ExcelUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

public class TeacherResolveTest {

    @Test
    public void testFormulaResolve() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("sheet1");
            Row row = sheet.createRow(0);
            Cell formulaCell = row.createCell(0);
            formulaCell.setCellFormula("1+1");

            String value = ExcelUtils.getExcelFormulaEvaString(sheet, formulaCell);
            org.junit.jupiter.api.Assertions.assertEquals("2", value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
