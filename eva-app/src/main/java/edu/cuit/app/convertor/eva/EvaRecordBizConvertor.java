package edu.cuit.app.convertor.eva;

import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import edu.cuit.client.dto.data.course.CourseTime;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
import edu.cuit.infra.convertor.EntityFactory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;


/**
 * 评教记录对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EvaRecordBizConvertor {
    @Mappings({
            @Mapping(target = "week",expression="java(singleCourseEntity.getWeek())"),
            @Mapping(target = "day",expression= "java(singleCourseEntity.getDay())"),
            @Mapping(target = "startTime",expression= "java(singleCourseEntity.getStartTime())"),
            @Mapping(target = "endTime",expression= "java(singleCourseEntity.getEndTime())"),
    })
    CourseTime toCourseTime(SingleCourseEntity singleCourseEntity);
    @Mappings({
            @Mapping(target = "id",expression="java(evaRecordEntity.getId())"),
            @Mapping(target = "teacherName",expression = "java(evaRecordEntity.getTask().getCourInf().getCourseEntity().getTeacher().getName())"),
            @Mapping(target = "evaTeacherName",expression = "java(evaRecordEntity.getTask().getTeacher().getName())"),
            @Mapping(target = "courseName",expression= "java(evaRecordEntity.getTask().getCourInf().getCourseEntity().getSubjectEntity().getName())"),
            @Mapping(target = "textValue",expression="java(evaRecordEntity.getTextValue())"),
            @Mapping(target = "formPropsValues",expression="java(evaRecordEntity.getFormPropsValues())"),
            @Mapping(target = "createTime",expression="java(evaRecordEntity.getCreateTime())"),
            @Mapping(target = "courseTime",expression="java(toCourseTime(evaRecordEntity.getTask().getCourInf()))"),
            @Mapping(target = "averScore",ignore = true),
            @Mapping(target = "topic",expression="java(evaRecordEntity.getTopic())"),
    })
    EvaRecordCO evaRecordEntityToCo(EvaRecordEntity evaRecordEntity);
}
