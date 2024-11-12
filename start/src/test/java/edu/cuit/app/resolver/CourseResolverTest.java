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
        File file = new File("D:\\Programming\\Java\\Projects\\evaluate-system\\2024-2025第一学期课表06周发布(实验课).xlsx");
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
        File file = new File("D:\\Programming\\Java\\Projects\\evaluate-system\\2023-2024-2学期教师课表.xlsx");
        List<CourseExcelBO> courseExcelBOS = CourseExcelResolver.resolveData(CourseExcelResolver.Strategy.THEORY_COURSE,
                new BufferedInputStream(new FileInputStream(file))).stream()
                .toList();
        for (CourseExcelBO courseExcelBO : courseExcelBOS) {
            System.out.println(courseExcelBO.getStartTime() + "-" + courseExcelBO.getEndTime() + " " + courseExcelBO.getWeeks());
        }
    }

}
