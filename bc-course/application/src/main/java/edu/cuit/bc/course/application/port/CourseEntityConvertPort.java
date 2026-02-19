package edu.cuit.bc.course.application.port;

import edu.cuit.domain.entity.course.CourseEntity;
import edu.cuit.domain.entity.course.SemesterEntity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.course.SubjectEntity;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SemesterDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;

import java.util.function.Supplier;

/**
 * 课程读侧：课程实体转换端口（用于收敛跨 BC 对 CourseConvertor 的直接依赖；保持行为不变）。
 */
public interface CourseEntityConvertPort {
    CourseEntity toCourseEntityWithTeacherObject(
            CourseDO courseDo,
            Supplier<SubjectEntity> subject,
            Supplier<?> teacher,
            Supplier<SemesterEntity> semester
    );

    SemesterEntity toSemesterEntity(SemesterDO semesterDO);

    SubjectEntity toSubjectEntity(SubjectDO subjectDO);

    SingleCourseEntity toSingleCourseEntity(Supplier<CourseEntity> course, CourInfDO courInfDo);
}

