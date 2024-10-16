package edu.cuit.app.resolver.course;

import edu.cuit.client.bo.CourseExcelBO;

import java.io.InputStream;
import java.util.List;

/**
 * 实验课excel读取实现
 */
public class ExperimentalCourseResolver extends CourseExcelResolverStrategy{

    protected ExperimentalCourseResolver(InputStream excelFileStream) {
        this.excelFileStream = excelFileStream;
    }

    @Override
    public List<CourseExcelBO> readData() {
        return List.of();
    }
}
