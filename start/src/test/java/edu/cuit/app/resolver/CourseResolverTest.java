package edu.cuit.app.resolver;

import edu.cuit.app.poi.course.CourseExcelResolver;
import edu.cuit.client.bo.CourseExcelBO;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

public class CourseResolverTest {

    @Test
    public void testExpResolve() {
        File file = new File("D:\\Downloads\\qq\\同行听课实验课表.xlsx");
        try {
            List<CourseExcelBO> courseExcelBOS = CourseExcelResolver.resolveData(CourseExcelResolver.Strategy.EXPERIMENTAL_COURSE,
                    new BufferedInputStream(new FileInputStream(file)));
            List<CourseExcelBO> courses = courseExcelBOS.stream()
                    .toList();
            for (CourseExcelBO cours : courses) {
                System.out.println(cours);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testTheoryResolve() throws FileNotFoundException {
        File file = new File("D:\\Downloads\\qq\\同行听课导入课表 (1).xlsx");
        List<CourseExcelBO> courseExcelBOS = CourseExcelResolver.resolveData(CourseExcelResolver.Strategy.THEORY_COURSE,
                new BufferedInputStream(new FileInputStream(file))).stream()
                .toList();
        for (CourseExcelBO courseExcelBO : courseExcelBOS) {
            System.out.println(courseExcelBO.getStartTime() + "-" + courseExcelBO.getEndTime() + " " + courseExcelBO.getWeeks());
        }
    }

}
