package edu.cuit.app.poi.eva;

import edu.cuit.app.poi.util.ExcelUtils;
import lombok.Getter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class EvaStatisticsExporter {

    @Getter
    protected final XSSFWorkbook workbook;

    protected final CellStyle headerStyle;
    protected final CellStyle contentStyle;

    protected int semId;

    public EvaStatisticsExporter() {
        workbook = new XSSFWorkbook();
        headerStyle = ExcelUtils.getHeaderStyle(workbook);
        contentStyle = ExcelUtils.getContentStyle(workbook);
    }

    protected void setSemesterId(int semId) {
        this.semId = semId;
    }

    protected void process() {}

}
