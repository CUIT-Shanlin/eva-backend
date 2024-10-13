package edu.cuit.app.resolver.course;

import com.alibaba.cola.exception.SysException;
import edu.cuit.app.bo.CourseExcelBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 课程解析器
 */
@Component
@Slf4j
public class CourseExcelResolver {

    public List<CourseExcelBO> resolveData(Strategy strategy, File data) {
        try {
            if (strategy == Strategy.THEORY_COURSE) {
                return new TheoryCourseExcelResolver(data).readData();
            }
            if (strategy == Strategy.EXPERIMENTAL_COURSE) {
                return new ExperimentalCourseResolver(data).readData();
            }
        } catch (IOException e) {
            log.error("读取excel出错",e);
            throw new SysException("读取excel出错，请联系管理员");
        }
        log.error("读取excel策略不存在");
        throw new SysException("读取策略不存在，请联系管理员");
    }


    public enum Strategy {
        // 理论课
        THEORY_COURSE,
        // 实验课
        EXPERIMENTAL_COURSE

    }

}
