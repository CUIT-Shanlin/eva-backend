package edu.cuit.app.poi.course;

import com.alibaba.cola.exception.SysException;
import edu.cuit.client.bo.CourseExcelBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 兼容旧包路径的 facade，避免现阶段影响服务层调用入口。
 */
@Component
@Slf4j
public class CourseExcelResolver {

    public static List<CourseExcelBO> resolveData(Strategy strategy, InputStream data) {
        if (strategy == Strategy.THEORY_COURSE) {
            return edu.cuit.app.poi.course.excel.CourseExcelResolver.resolveData(
                    edu.cuit.app.poi.course.excel.CourseExcelResolver.Strategy.THEORY_COURSE,
                    data
            );
        }
        if (strategy == Strategy.EXPERIMENTAL_COURSE) {
            return edu.cuit.app.poi.course.excel.CourseExcelResolver.resolveData(
                    edu.cuit.app.poi.course.excel.CourseExcelResolver.Strategy.EXPERIMENTAL_COURSE,
                    data
            );
        }

        log.error("读取excel策略不存在");
        throw new SysException("读取策略不存在，请联系管理员");
    }

    public enum Strategy {
        THEORY_COURSE,
        EXPERIMENTAL_COURSE
    }
}
