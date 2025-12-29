package edu.cuit.app.poi.course;

import com.alibaba.cola.exception.BizException;
import com.alibaba.cola.exception.SysException;
import edu.cuit.client.bo.CourseExcelBO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 课程解析器
 */
@Component
@Slf4j
public class CourseExcelResolver {

    public static List<CourseExcelBO> resolveData(Strategy strategy, InputStream data) {
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
        } catch (InvalidFormatException e) {
            throw new BizException("excel文件格式不正确");
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
