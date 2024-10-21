package edu.cuit.app.convertor.eva;

import edu.cuit.client.dto.clientobject.eva.EvaInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskBaseInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskDetailInfoCO;
import edu.cuit.client.dto.data.course.CourseTime;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
import edu.cuit.infra.convertor.EntityFactory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",uses = EntityFactory.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EvaTaskBizConvertor {
    EvaInfoCO evaTaskEntityToEvaInfoCO(EvaTaskEntity evaTaskEntity);

    @Mappings({
            @Mapping(target = "week",expression="java(singleCourseEntity.getWeek())"),
            @Mapping(target = "day",expression= "java(singleCourseEntity.getDay())"),
            @Mapping(target = "startTime",expression= "java(singleCourseEntity.getStartTime())"),
            @Mapping(target = "endTime",expression= "java(singleCourseEntity.getEndTime())"),
    })
    CourseTime toCourseTime(SingleCourseEntity singleCourseEntity);

    @Mappings({
            @Mapping(target = "id",expression="java(evaTaskEntity.getId())"),
            @Mapping(target = "status",expression = "java(evaTaskEntity.getStatus())"),
            @Mapping(target = "evaTeacherName",expression = "java(evaTaskEntity.getTeacher().getName())"),
            @Mapping(target = "teacherName",expression= "java(evaTaskEntity.getCourInf().getCourseEntity().getTeacher().getName())"),
            @Mapping(target = "courseName",expression="java(evaTaskEntity.getCourInf().getCourseEntity().getSubjectEntity().getName())"),
            @Mapping(target = "createTime",expression="java(evaTaskEntity.getCreateTime())"),
            @Mapping(target = "updateTime",expression="java(evaTaskEntity.getUpdateTime())"),
            @Mapping(target = "courseTime",expression="java(toCourseTime(evaTaskEntity.getCourInf()))"),
            @Mapping(target = "location",expression="java(evaTaskEntity.getCourInf().getLocation())"),
    })
    EvaTaskDetailInfoCO evaTaskEntityToTaskDetailCO(EvaTaskEntity evaTaskEntity,SingleCourseEntity singleCourseEntity);

    @Mappings({
            @Mapping(target = "evaTeacherName",expression="java(evaTaskEntity.getTeacher().getName())"),
            @Mapping(target = "teacherName",expression= "java(evaTaskEntity.getCourInf().getCourseEntity().getTeacher().getName())"),
            @Mapping(target = "courseName",expression= "java(evaTaskEntity.getCourInf().getCourseEntity().getSubjectEntity().getName())"),
    })
    EvaTaskBaseInfoCO evaTaskEntityToEvaBaseCO(EvaTaskEntity evaTaskEntity);
}
