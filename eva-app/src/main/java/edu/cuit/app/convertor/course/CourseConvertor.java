package edu.cuit.app.convertor.course;

import edu.cuit.client.dto.clientobject.SimpleCourseResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.course.*;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.domain.entity.course.CourseEntity;
import edu.cuit.domain.entity.course.CourseTypeEntity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.course.SubjectEntity;
import edu.cuit.infra.convertor.EntityFactory;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.*;

/**
 * 课程业务对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseConvertor {

    @Mappings({
            @Mapping(target = "name",source = "subject.name"),
            @Mapping(target = "id",source = "id"),
            @Mapping(target = "teacherName",source = "teacher.username"),
    })
    SimpleCourseResultCO toSimpleCourseResultCO(CourseEntity entity);

    SimpleResultCO toSimpleResultCO(SubjectEntity entity);
    SimpleResultCO toSimpleResultCO(SelfTeachCourseCO entity);

    CourseType toCourseType(CourseTypeEntity courseTypeEntity);

//    SelfTeachCourseTimeCO toSelfCourseTimeCO(SingleCourseEntity );


 /*   @Mappings({
            @Mapping(target = "nature",source = "course.subject.nature"),
            @Mapping(target = "typeList",source = "id"),
            @Mapping(target = "teacherName",source = "teacher.username"),
    })
    SingleCourseDetailCO toSingleCourseDetailCO(SingleCourseEntity singleCourseEntity);*/
}
