package edu.cuit.app.resolver;

import edu.cuit.app.poi.course.util.ExcelUtils;
import edu.cuit.domain.entity.user.LdapPersonEntity;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;

@SpringBootTest
public class TeacherResolveTest {

    @Autowired
    public LdapPersonGateway ldapPersonGateway;

    @Test
    public void testResolve() throws IOException {
        File file = new File("D:\\Programming\\Java\\Projects\\evaluate-system\\data.xlsx");
        Sheet sheet = WorkbookFactory.create(file).getSheetAt(0);
        int rowIndex = 1;
        while (true) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) break;
            LdapPersonEntity entity = new LdapPersonEntity();
            Cell cell = row.getCell(1);
            String username = ExcelUtils.getCellStringValue(cell);
            String name = ExcelUtils.getCellStringValue(row.getCell(2));
            entity.setUsername(username);
            entity.setName(name);
            entity.setPhone(ExcelUtils.getExcelFormulaEvaString(sheet,row.getCell(4)));
            entity.setEmail(ExcelUtils.getCellStringValue(row.getCell(3)));
            entity.setTitle(ExcelUtils.getCellStringValue(row.getCell(5)));
            entity.setSchool(ExcelUtils.getCellStringValue(row.getCell(6)));
            entity.setSurname(name.substring(0,1));
            entity.setIsAdmin(false);
            ldapPersonGateway.createUser(entity,username);
            rowIndex++;
        }
    }

}
