package edu.cuit.infra.bccourse.adapter;

import edu.cuit.app.poi.course.CourseExcelResolver;
import edu.cuit.bc.course.application.port.CourseExcelResolvePort;
import edu.cuit.client.bo.CourseExcelBO;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 课表 Excel 解析端口适配器（基础设施端）。
 *
 * <p>过渡期保持行为不变：复用既有 {@link CourseExcelResolver} 的解析实现与异常/日志行为。</p>
 */
@Component
public class CourseExcelResolvePortImpl implements CourseExcelResolvePort {
    @Override
    public List<CourseExcelBO> resolveTheoryCourse(InputStream excelFileStream) {
        return CourseExcelResolver.resolveData(CourseExcelResolver.Strategy.THEORY_COURSE, excelFileStream);
    }

    @Override
    public List<CourseExcelBO> resolveExperimentalCourse(InputStream excelFileStream) {
        return CourseExcelResolver.resolveData(CourseExcelResolver.Strategy.EXPERIMENTAL_COURSE, excelFileStream);
    }
}
