package edu.cuit.app;

import edu.cuit.app.poi.util.ExcelUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

public class ExcelExporterTest {

    @Test
    public void testHeaderStyleAndMerge() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("sheet1");
            sheet.createRow(0).createCell(0).setCellValue("title");

            CellStyle headerStyle = ExcelUtils.getHeaderStyle(workbook);
            ExcelUtils.createRegion(0, 0, 0, 1, sheet);

            org.junit.jupiter.api.Assertions.assertNotNull(headerStyle);
            org.junit.jupiter.api.Assertions.assertEquals(1, sheet.getNumMergedRegions());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
