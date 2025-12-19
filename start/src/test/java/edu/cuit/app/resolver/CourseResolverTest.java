package edu.cuit.app.resolver;

import com.alibaba.cola.exception.BizException;
import edu.cuit.app.poi.course.CourseExcelResolver;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CourseResolverTest {

    @Test
    public void testExpResolve_invalidFormat_shouldThrow() throws IOException {
        ByteArrayInputStream inputStream = buildEmptyWorkbook();
        BizException ex = org.junit.jupiter.api.Assertions.assertThrows(BizException.class, () ->
                CourseExcelResolver.resolveData(CourseExcelResolver.Strategy.EXPERIMENTAL_COURSE, inputStream)
        );
        org.junit.jupiter.api.Assertions.assertEquals("实验课程表格格式有误", ex.getMessage());
    }

    @Test
    public void testTheoryResolve_invalidFormat_shouldThrow() throws IOException {
        ByteArrayInputStream inputStream = buildEmptyWorkbook();
        BizException ex = org.junit.jupiter.api.Assertions.assertThrows(BizException.class, () ->
                CourseExcelResolver.resolveData(CourseExcelResolver.Strategy.THEORY_COURSE, inputStream)
        );
        org.junit.jupiter.api.Assertions.assertEquals("理论课程表格格式有误", ex.getMessage());
    }

    private ByteArrayInputStream buildEmptyWorkbook() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.createSheet("sheet1");
            workbook.write(outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }

}
