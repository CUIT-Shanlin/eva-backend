package edu.cuit.app.poi.eva;

import cn.hutool.extra.spring.SpringUtil;
import edu.cuit.app.poi.util.ExcelUtils;
import edu.cuit.client.api.eva.IEvaConfigService;
import edu.cuit.client.dto.data.EvaConfig;
import edu.cuit.domain.entity.user.biz.UserEntity;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;

import java.util.List;

/**
 * 用户任务完成情况sheet
 */
public class FillUserStatisticsExporterDecorator extends EvaStatisticsExporter {

    private final static IEvaConfigService evaConfigService;

    static {
        evaConfigService = SpringUtil.getBean(IEvaConfigService.class);
    }

    public FillUserStatisticsExporterDecorator(EvaStatisticsExporter exporter) {
        super(exporter.semId);
        this.exporter = exporter;
    }

    private EvaConfig evaConfig;
    private XSSFCellStyle unqualifiedCellStyle;
    private XSSFCellStyle noCourseBeEvaUnqualifiedCellStyle;

    @Override
    protected void process() {
        exporter.process();
        workbook = exporter.getWorkbook();
        sheet = workbook.createSheet("任务统计");
        evaConfig = evaConfigService.getEvaConfig();

        // 创建未达标样式
        unqualifiedCellStyle = workbook.createCellStyle();
        unqualifiedCellStyle.cloneStyleFrom(getContentStyle());
        unqualifiedCellStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        unqualifiedCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 创建虽然未达标但没有办法因为本学期没有课程的样式
        noCourseBeEvaUnqualifiedCellStyle= workbook.createCellStyle();
        noCourseBeEvaUnqualifiedCellStyle.cloneStyleFrom(getContentStyle());
        noCourseBeEvaUnqualifiedCellStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        noCourseBeEvaUnqualifiedCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

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

    private int rowIndex = 2;

    private void insertTeacherData(UserEntity teacher) {
        //去查老师这学期有没有课程
        List<Integer> courseIds = userCourseService.getUserCourses(semId, teacher.getId());

        Row teacherRow = createRow(rowIndex);

        createCell(teacherRow,0).setCellValue(teacher.getName());

        List<Integer> count = evaQueryGateway.getCountAbEva(semId, teacher.getId());

        ExcelUtils.createRegion(rowIndex,rowIndex,0,1,sheet);

        // 评教次数处理
        createCell(teacherRow,2).setCellValue(count.get(0));
        int unqualifiedEvaCount = evaConfig.getMinEvaNum() - count.get(0);
        createCell(teacherRow,3).setCellValue(Math.max(unqualifiedEvaCount, 0));
        Cell isEvaQualifiedCell = createCell(teacherRow, 4);
        //评教次数
        if (unqualifiedEvaCount > 0) {
            isEvaQualifiedCell.setCellStyle(unqualifiedCellStyle);
            isEvaQualifiedCell.setCellValue("否");
        } else isEvaQualifiedCell.setCellValue("是");

        createCell(teacherRow,5).setCellValue(count.get(1));
        int unqualifiedBeEvaCount = evaConfig.getMinBeEvaNum() - count.get(1);
        createCell(teacherRow,6).setCellValue(Math.max(unqualifiedBeEvaCount,0));
        Cell isBeEvaQualifiedCell = createCell(teacherRow, 7);
        //被评教次数(只有待被评教次数>0且老师确实这学期有课才不达标)
        if (unqualifiedBeEvaCount > 0) {
            if(courseIds.size()==0){
                isBeEvaQualifiedCell.setCellStyle(noCourseBeEvaUnqualifiedCellStyle);//变绿特效
                isBeEvaQualifiedCell.setCellValue("是");
            }else {
                isBeEvaQualifiedCell.setCellStyle(unqualifiedCellStyle);//变黄特效
                isBeEvaQualifiedCell.setCellValue("否");
            }
        } else isBeEvaQualifiedCell.setCellValue("是");

        rowIndex++;
    }

    private Row createRow(int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) row = sheet.createRow(rowIndex);
        return row;
    }

    private void createHeader() {
        addTitle("教师任务完成情况统计",7);

        Row headerRow = sheet.createRow(1);
        headerRow.setHeight((short)(24*25));

        // 处理教师格
        createHeaderCell(0,1,"教师",headerRow);
        createHeaderCell(2,2,"已评教次数",headerRow);
        createHeaderCell(3,3,"待评教次数",headerRow);
        createHeaderCell(4,4,"是否达标",headerRow);
        createHeaderCell(5,5,"已被评教次数",headerRow);
        createHeaderCell(6,6,"待被评教次数",headerRow);
        createHeaderCell(7,7,"是否达标",headerRow);
        createHeaderCell(8,9,"评教达标数：" + evaConfig.getMinEvaNum() + "\n" +
                "被评教达标数：" + evaConfig.getMinBeEvaNum(),headerRow);

    }
}
