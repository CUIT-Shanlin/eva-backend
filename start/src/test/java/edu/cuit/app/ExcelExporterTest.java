package edu.cuit.app;

import edu.cuit.app.poi.eva.EvaStatisticsExcelFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;

@SpringBootTest
public class ExcelExporterTest {

    @Test
    public void testExport() throws IOException {

        File file = new File("D:\\Programming\\Java\\Projects\\evaluate-system\\评教数据.xlsx");
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        EvaStatisticsExcelFactory.createExcelData(1).write(out);
        out.close();
    }

}
