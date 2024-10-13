package edu.cuit.app.resolver.course;

import edu.cuit.app.bo.CourseExcelBO;

import java.io.File;
import java.util.List;

/**
 * 实验课excel读取实现
 */
public class ExperimentalCourseResolver extends CourseExcelResolverStrategy{

    protected ExperimentalCourseResolver(File excelFile) {
        this.excelFile = excelFile;
    }

    @Override
    public List<CourseExcelBO> readData() {
        return List.of();
    }
}
