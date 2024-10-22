package edu.cuit.app.convertor.course;

import edu.cuit.client.dto.clientobject.SimpleCourseResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
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
            @Mapping(target = "id",source = "id"),
            @Mapping(target = "teacherName",expression= "java(entity.getTeacher().getName())"),
    })
    public abstract SimpleCourseResultCO toSimpleCourseResultCO(CourseEntity entity);

    public abstract SimpleResultCO toSimpleResultCO(SubjectEntity entity);
    public abstract SimpleResultCO toSimpleResultCO(SelfTeachCourseCO entity);

    public abstract CourseType toCourseType(CourseTypeEntity courseTypeEntity);

    @Mappings({
            @Mapping(target = "id",source = "courseEntity.id"),
            @Mapping(target = "classroomList",source = "classroomList"),
            @Mapping(target = "createTime",source = "courseEntity.createTime"),
            @Mapping(target = "updateTime",source = "courseEntity.updateTime"),
            @Mapping(target = "name",expression="java(courseEntity.getSubjectEntity().getName())"),
            @Mapping(target = "templateMsg",expression="java(toEvaTemplateCO(formTemplateMapper.selectById(courseEntity.getTemplateId())))"),
            @Mapping(target = "teacherInfoCO",expression="java(toTeacherInfoCO(courseEntity.getTeacher()))")
    })
    public abstract CourseModelCO toCourseModelCO(CourseEntity courseEntity, List<String> classroomList);

    public abstract EvaTemplateCO toEvaTemplateCO(FormTemplateDO formTemplateDO);
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
            @Mapping(target = "evaNum",source = "size")
    })
    public abstract SingleCourseCO toSingleCourseCO(SingleCourseEntity singleCourseEntity,Integer size);

    public abstract CourseTime toCourseTime(SingleCourseEntity singleCourseEntity);

}
