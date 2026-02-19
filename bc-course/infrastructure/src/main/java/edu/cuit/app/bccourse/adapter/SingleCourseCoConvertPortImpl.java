package edu.cuit.app.bccourse.adapter;

import edu.cuit.app.convertor.course.CourseBizConvertor;
import edu.cuit.bc.course.application.port.SingleCourseCoConvertPort;
import edu.cuit.client.dto.clientobject.course.SingleCourseCO;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * bc-course：单节课 CO 转换端口适配器（复用既有 CourseBizConvertor，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class SingleCourseCoConvertPortImpl implements SingleCourseCoConvertPort {
    private final CourseBizConvertor courseBizConvertor;

    @Override
    public SingleCourseCO toSingleCourseCO(SingleCourseEntity singleCourseEntity, Integer evaNum) {
        return courseBizConvertor.toSingleCourseCO(singleCourseEntity, evaNum);
    }
}

