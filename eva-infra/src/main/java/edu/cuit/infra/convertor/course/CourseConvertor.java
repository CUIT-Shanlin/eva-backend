package edu.cuit.infra.convertor.course;

import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.course.SubjectCO;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.client.dto.cmd.course.UpdateSingleCourseCmd;
import edu.cuit.client.dto.data.course.CoursePeriod;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.domain.entity.course.*;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.infra.convertor.EntityFactory;
import edu.cuit.infra.dal.database.dataobject.course.*;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring", uses = {EntityFactory.class})
public interface CourseConvertor {

    @Mappings({
            @Mapping(target = "id",source = "courseDo.id"),
            @Mapping(target = "subject",source = "subject"),
            @Mapping(target = "teacher",source = "teacher"),
            @Mapping(target = "classroom",source = "courseDo.classroom"),
            @Mapping(target = "semester",source = "semester"),
            @Mapping(target = "createTime",source = "courseDo.createTime"),
            @Mapping(target = "updateTime",source = "courseDo.updateTime"),
            @Mapping(target = "isDeleted",source = "courseDo.isDeleted")
    })

    CourseEntity toCourseEntity(CourseDO courseDo, SubjectEntity subject, UserEntity teacher,SemesterEntity semester);
    CourseTypeEntity toCourseTypeEntity(CourseTypeDO courseTypeDO);
    SemesterEntity toSemesterEntity(SemesterDO semesterDO);
    @Mappings({
            @Mapping(target = "id",source = "courInfDo.id"),
            @Mapping(target = "course",source = "course"),
            @Mapping(target = "week",source = "courInfDo.week"),
            @Mapping(target = "startTime",source = "courInfDo.startTime"),
            @Mapping(target = "endTime",source = "courInfDo.endTime"),
            @Mapping(target = "createTime",source = "courInfDo.createTime"),
            @Mapping(target ="updateTime",source = "courInfDo.updateTime"),
            @Mapping(target ="isDeleted",source = "courInfDo.isDeleted"),
            @Mapping(target ="location",source = "courInfDo.location"),
            @Mapping(target ="day",source = "courInfDo.day")
    })
    SingleCourseEntity toSingleCourseEntity(CourseEntity course, CourInfDO courInfDo);
    SubjectEntity toSubjectEntity(SubjectDO subjectDO);
    @Mappings({
            @Mapping(target = "typeList",source = "typeList"),
            @Mapping(target = "dateList",source = "dateList"),
            @Mapping(target = "courseBaseMsg.id",source = "courInfo.courseId"),
            @Mapping(target = "courseBaseMsg.classroomList",source = "course.classRoom"),
            @Mapping(target = "courseBaseMsg.name",source = "courInfo.name"),
            @Mapping(target = "courseBaseMsg.createTime",source = "courInfo.createTime"),
            @Mapping(target = "courseBaseMsg.updateTime",source = "courInfo.updateTime"),
            @Mapping(target = "courseBaseMsg.templateMsg",source = "template"),
            @Mapping(target = "courseBaseMsg.teacherInfoCO.id",source = "user.id"),
            @Mapping(target = "courseBaseMsg.teacherInfoCO.name",source = "user.username"),
            @Mapping(target = "courseBaseMsg.teacherInfoCO.department",source = "user.department"),

    })
    CourseDetailCO toCourseDetailCO(List<CourseType> typeList, List<CoursePeriod> dateList, SubjectDO courInfo
                                    , CourseDO course, EvaTemplateCO template, SysUserDO user);
    @Mappings({
            @Mapping(target = "id",source = "id"),
    })
    CourseType toCourseType(Integer id,CourseTypeDO courseTypeDO);
    SubjectDO toSubjectDO(SubjectCO subjectDO);
    @Mappings({
            @Mapping(target = "week",source = "time.week"),
            @Mapping(target = "day",source = "time.day"),
            @Mapping(target = "startTime",source = "time.startTime"),
            @Mapping(target = "endTime",source = "time.endTime"),
    })
    CourInfDO toCourInfDO(UpdateSingleCourseCmd singleCourse);
    CourseTypeDO toCourseTypeDO(CourseType courseType);




}
