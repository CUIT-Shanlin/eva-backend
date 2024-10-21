package edu.cuit.app.resolver.course;

import edu.cuit.client.bo.CourseExcelBO;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 课程表格解析
 */
public abstract class CourseExcelResolverStrategy {

    protected InputStream excelFileStream;

    /**
     * 读取数据
     * @return 课程列表
     */
    public abstract List<CourseExcelBO> readData() throws IOException, InvalidFormatException;

}
