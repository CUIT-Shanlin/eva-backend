package edu.cuit.bc.course.application.port;

import edu.cuit.client.dto.clientobject.course.SingleCourseCO;
import edu.cuit.domain.entity.course.SingleCourseEntity;

/**
 * 课程读侧：单节课 CO 转换端口（用于收敛跨 BC 对 Convertor 的直接依赖；保持行为不变）。
 */
public interface SingleCourseCoConvertPort {
    SingleCourseCO toSingleCourseCO(SingleCourseEntity singleCourseEntity, Integer evaNum);
}

