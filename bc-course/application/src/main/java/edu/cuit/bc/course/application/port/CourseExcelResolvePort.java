package edu.cuit.bc.course.application.port;

import edu.cuit.client.bo.CourseExcelBO;

import java.io.InputStream;
import java.util.List;

/**
 * 课表 Excel 解析端口（由基础设施层实现）。
 *
 * <p>过渡期保持行为不变：由端口适配器复用既有 POI 解析实现，确保异常文案与日志顺序不变。</p>
 */
public interface CourseExcelResolvePort {
    /**
     * 解析理论课课表 Excel。
     *
     * @param excelFileStream excel 文件流
     * @return 解析出的课表行
     */
    List<CourseExcelBO> resolveTheoryCourse(InputStream excelFileStream);

    /**
     * 解析实验课课表 Excel。
     *
     * @param excelFileStream excel 文件流
     * @return 解析出的课表行
     */
    List<CourseExcelBO> resolveExperimentalCourse(InputStream excelFileStream);
}
