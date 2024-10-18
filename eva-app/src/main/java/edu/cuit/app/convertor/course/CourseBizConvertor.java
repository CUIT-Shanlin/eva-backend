package edu.cuit.app.convertor.course;

import edu.cuit.client.dto.clientobject.SimpleCourseResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.course.*;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.domain.entity.course.CourseEntity;
import edu.cuit.domain.entity.course.CourseTypeEntity;
import edu.cuit.domain.entity.course.SubjectEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
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
@Mapper(componentModel = "spring",uses = {EntityFactory.class,FormTemplateMapper.class},unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class CourseBizConvertor {

    @Autowired
    FormTemplateMapper formTemplateMapper;

    @Mappings({
            @Mapping(target = "name",expression = "java(entity.getSubjectEntity().getName())"),
            @Mapping(target = "id",source = "id"),
            @Mapping(target = "teacherName",expression = "java(entity.getTeacher().getName())"),
    })
    public abstract SimpleCourseResultCO toSimpleCourseResultCO(CourseEntity entity);

    public abstract SimpleResultCO toSimpleResultCO(SubjectEntity entity);
    public abstract SimpleResultCO toSimpleResultCO(SelfTeachCourseCO entity);

    public abstract CourseType toCourseType(CourseTypeEntity courseTypeEntity);

    @Mappings({
            @Mapping(target = "id",source = "courseEntity.id"),
            @Mapping(target = "classroomList",source = "classroomList"),
            @Mapping(target = "createTime",source = "courseEntity.createTime"),
            @Mapping(target = "name",expression="java(courseEntity.getSubjectEntity().getName())"),
            @Mapping(target = "templateMsg",expression="java(toEvaTemplateCO(formTemplateMapper.selectById(courseEntity.getTemplateId())))"),
            @Mapping(target = "teacherInfoCO",expression="java(toTeacherInfoCO(courseEntity.getTeacher()))"),
    })
    public abstract CourseModelCO toCourseModelCO(CourseEntity courseEntity, List<String> classroomList);

     abstract EvaTemplateCO toEvaTemplateCO(FormTemplateDO formTemplateDO);
     abstract TeacherInfoCO toTeacherInfoCO(UserEntity userEntity);
}
