package edu.cuit.app.resolver;

import edu.cuit.client.bo.CourseExcelBO;
import edu.cuit.app.resolver.course.CourseExcelResolver;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

public class ExcelResolverTest {

    @Test
    public void testResolveClass() {
        File file = new File("D:\\Programming\\Java\\Projects\\evaluate-system\\2023-2024-2学期教师课表.xlsx");
        List<CourseExcelBO> courseExcelBOS = null;
        try {
            courseExcelBOS = CourseExcelResolver.resolveData(CourseExcelResolver.Strategy.THEORY_COURSE, new BufferedInputStream(new FileInputStream(file)));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        for (CourseExcelBO courseExcelBO : courseExcelBOS) {
//            if (courseExcelBO.getCourseName().equals("分布式系统及云计算技术")) {
                System.out.println(courseExcelBO.getCourseName());
                System.out.println(courseExcelBO.getDay());
                System.out.println(courseExcelBO.getTeacherName());
                System.out.println(courseExcelBO.getWeeks());
                System.out.println(courseExcelBO.getStartTime());
                System.out.println(courseExcelBO.getEndTime());
                System.out.println("===============");
//            }
        }
    }

}
