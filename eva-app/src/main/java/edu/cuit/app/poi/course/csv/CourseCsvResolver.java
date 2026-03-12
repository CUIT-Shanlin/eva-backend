package edu.cuit.app.poi.course.csv;

import com.alibaba.cola.exception.BizException;
import edu.cuit.client.bo.CourseExcelBO;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 课程 CSV 解析器
 */
@Slf4j
public final class CourseCsvResolver {

    private CourseCsvResolver() {
    }

    public static List<CourseExcelBO> resolveData(Strategy strategy, InputStream data) {
        try {
            return buildResolver(strategy, data).readData();
        } catch (IOException e) {
            log.error("读取csv出错", e);
            throw new BizException("读取csv出错，请联系管理员");
        }
    }

    static CourseCsvResolverStrategy<?> buildResolver(Strategy strategy, InputStream data) {
        if (strategy == Strategy.THEORY_COURSE) {
            return new TheoryCourseCsvResolver(data);
        }
        if (strategy == Strategy.EXPERIMENTAL_COURSE) {
            return new ExperimentalCourseCsvResolver(data);
        }
        log.error("读取csv策略不存在");
        throw new BizException("读取策略不存在，请联系管理员");
    }

    public enum Strategy {
        THEORY_COURSE,
        EXPERIMENTAL_COURSE
    }
}
