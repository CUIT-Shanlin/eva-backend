package edu.cuit.app.bccourse.adapter;

import edu.cuit.bc.course.application.port.CourseEntityConvertPort;
import edu.cuit.domain.entity.course.CourseEntity;
import edu.cuit.domain.entity.course.SemesterEntity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.course.SubjectEntity;
import edu.cuit.infra.convertor.course.CourseConvertor;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SemesterDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * bc-course：课程实体转换端口适配器（复用既有 CourseConvertor，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class CourseEntityConvertPortImpl implements CourseEntityConvertPort {
    private final CourseConvertor courseConvertor;

    @Override
    public CourseEntity toCourseEntityWithTeacherObject(
            CourseDO courseDo,
            Supplier<SubjectEntity> subject,
            Supplier<?> teacher,
            Supplier<SemesterEntity> semester
    ) {
        return courseConvertor.toCourseEntityWithTeacherObject(courseDo, subject, teacher, semester);
    }

    @Override
    public SemesterEntity toSemesterEntity(SemesterDO semesterDO) {
        return courseConvertor.toSemesterEntity(semesterDO);
    }

    @Override
    public SubjectEntity toSubjectEntity(SubjectDO subjectDO) {
        return courseConvertor.toSubjectEntity(subjectDO);
    }

    @Override
    public SingleCourseEntity toSingleCourseEntity(Supplier<CourseEntity> course, CourInfDO courInfDo) {
        return courseConvertor.toSingleCourseEntity(course, courInfDo);
    }
}

