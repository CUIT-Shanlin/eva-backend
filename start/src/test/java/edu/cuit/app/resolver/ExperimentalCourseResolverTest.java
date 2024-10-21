package edu.cuit.app.resolver;

import edu.cuit.app.resolver.course.CourseExcelResolver;
import edu.cuit.client.bo.CourseExcelBO;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

public class ExperimentalCourseResolverTest {

    @Test
    public void testExpResolve() {
        File file = new File("D:\\Programming\\Java\\Projects\\evaluate-system\\2024-2025第一学期课表06周发布(实验课).xlsx");
        try {
            List<CourseExcelBO> courseExcelBOS = CourseExcelResolver.resolveData(CourseExcelResolver.Strategy.EXPERIMENTAL_COURSE,
                    new BufferedInputStream(new FileInputStream(file)));
            System.out.println(courseExcelBOS.stream().filter(courseExcelBO ->
                    courseExcelBO.getTeacherName().equals("蒋瑜") && courseExcelBO.getDay() == 2)
                    .toList());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testTheoryResolve() throws FileNotFoundException {
        File file = new File("D:\\Programming\\Java\\Projects\\evaluate-system\\2023-2024-2学期教师课表.xlsx");
        List<CourseExcelBO> courseExcelBOS = CourseExcelResolver.resolveData(CourseExcelResolver.Strategy.THEORY_COURSE,
                new BufferedInputStream(new FileInputStream(file)));
        System.out.println(courseExcelBOS.toString());
    }

}
