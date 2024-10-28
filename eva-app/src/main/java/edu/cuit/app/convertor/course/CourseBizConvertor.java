package edu.cuit.app.convertor.course;

import edu.cuit.client.dto.clientobject.SimpleCourseResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.SimpleSubjectResultCO;
import edu.cuit.client.dto.clientobject.course.*;
import edu.cuit.client.dto.clientobject.eva.EvaTeacherInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.client.dto.data.course.CourseTime;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.domain.entity.course.CourseEntity;
import edu.cuit.domain.entity.course.CourseTypeEntity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.course.SubjectEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.infra.convertor.EntityFactory;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 课程业务对象转换器
 */
@Mapper(componentModel = "spring",uses = {EntityFactory.class,FormTemplateMapper.class, CourseQueryGateway.class},unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class CourseBizConvertor {
    @Autowired
    protected FormTemplateMapper formTemplateMapper;
    @Autowired
    protected CourseQueryGateway courseQueryGateway;

    @Mappings({
            @Mapping(target = "name",expression = "java(entity.getSubjectEntity().getName())"),
            @Mapping(target = "id",expression = "java(entity.getId())"),
            @Mapping(target = "teacherName",expression= "java(entity.getTeacher().getName())"),
            @Mapping(target = "nature",expression= "java(entity.getSubjectEntity().getNature())"),
    })
    public abstract SimpleCourseResultCO toSimpleCourseResultCO(CourseEntity entity);

    @Mappings({

            @Mapping(target = "id",expression = "java(entity.getId())"),
            @Mapping(target = "name",expression = "java(entity.getName())"),
            @Mapping(target = "nature",expression = "java(entity.getNature())")

    })
    public abstract SimpleSubjectResultCO toSimpleSubjectResultCO(SubjectEntity entity);
    @Mappings({

            @Mapping(target = "id",expression = "java(entity.getId())")

    })
    public abstract SimpleResultCO toSimpleResultCO(SelfTeachCourseCO entity);
    @Mappings({

            @Mapping(target = "id",expression = "java(courseTypeEntity.getId())"),
            @Mapping(target = "isDefault",expression = "java(courseTypeEntity.getIsDefault())"),

    })

    public abstract CourseType toCourseType(CourseTypeEntity courseTypeEntity);

    @Mappings({
            @Mapping(target = "id",expression = "java(courseEntity.getId())"),
            @Mapping(target = "classroomList",source = "classroomList"),
            @Mapping(target = "createTime",source = "courseEntity.createTime"),
            @Mapping(target = "updateTime",source = "courseEntity.updateTime"),
            @Mapping(target = "name",expression="java(courseEntity.getSubjectEntity().getName())"),
            @Mapping(target = "templateMsg",expression="java(toEvaTemplateCO(formTemplateMapper.selectById(courseEntity.getTemplateId())))"),
            @Mapping(target = "teacherInfoCO",expression="java(toTeacherInfoCO(courseEntity.getTeacher()))")
    })
    public abstract CourseModelCO toCourseModelCO(CourseEntity courseEntity, List<String> classroomList);

    @Mappings({

            @Mapping(target = "id",expression = "java(formTemplateDO.getId())"),
            @Mapping(target = "isDefault",expression = "java(formTemplateDO.getIsDefault())"),

    })
    public abstract EvaTemplateCO toEvaTemplateCO(FormTemplateDO formTemplateDO);
    @Mappings({

            @Mapping(target = "id",expression = "java(userEntity.getId())"),


    })
    public abstract TeacherInfoCO toTeacherInfoCO(UserEntity userEntity);
    @Mappings({
            @Mapping(target = "location",source = "singleCourseEntity.location"),
            @Mapping(target = "nature",expression = "java(singleCourseEntity.getCourseEntity().getSubjectEntity().getNature())"),
            @Mapping(target = "typeList",expression = "java(courseQueryGateway.getCourseType(singleCourseEntity.getCourseEntity().getId()))"),
            @Mapping(target = "evaTeachers",source = "evaTeachers"),
            @Mapping(target = "course",expression = "java(toSingleCourseCO(singleCourseEntity,evaTeachers.size()))"),
            @Mapping(target = "time",expression = "java(toCourseTime(singleCourseEntity))")
    })
    public abstract SingleCourseDetailCO toSingleCourseDetailCO(SingleCourseEntity singleCourseEntity,List<EvaTeacherInfoCO> evaTeachers);


    @Mappings({
            @Mapping(target = "id",expression = "java(singleCourseEntity.getCourseEntity().getId())"),
            @Mapping(target = "name",expression = "java(singleCourseEntity.getCourseEntity().getSubjectEntity().getName())"),
            @Mapping(target = "teacherName",expression = "java(singleCourseEntity.getCourseEntity().getTeacher().getName())"),
            @Mapping(target = "evaNum",expression = "java(size)")
    })
    public abstract SingleCourseCO toSingleCourseCO(SingleCourseEntity singleCourseEntity,Integer size);


    @Mappings({
            @Mapping(target = "week",expression = "java(singleCourseEntity.getWeek())"),
            @Mapping(target = "day",expression = "java(singleCourseEntity.getDay())"),
            @Mapping(target = "startTime",expression = "java(singleCourseEntity.getStartTime())"),
            @Mapping(target = "endTime",expression = "java(singleCourseEntity.getEndTime())")
    })
    public abstract CourseTime toCourseTime(SingleCourseEntity singleCourseEntity);

}
