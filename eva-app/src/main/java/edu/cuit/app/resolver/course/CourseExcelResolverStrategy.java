package edu.cuit.app.resolver.course;

import edu.cuit.app.bo.CourseExcelBO;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 课程表格解析
 */
public abstract class CourseExcelResolverStrategy {

    protected File excelFile;

    /**
     * 读取数据
     * @return 课程列表
     */
    public abstract List<CourseExcelBO> readData() throws IOException;

}
