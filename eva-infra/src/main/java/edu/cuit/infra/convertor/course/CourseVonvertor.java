package edu.cuit.infra.convertor.course;

import edu.cuit.domain.entity.course.*;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.infra.dal.database.dataobject.course.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper
public interface CourseVonvertor {
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
    CourseEntity ToCourseEntity(CourseDO courseDo, SubjectEntity subject, UserEntity teacher,SemesterEntity semester);
    CourseTypeEntity ToCourseTypeEntity(CourseTypeDO courseTypeDO);
    SemesterEntity ToSemesterEntity(SemesterDO semesterDO);
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
    SingleCourseEntity ToSingleCourseEntity(CourseEntity course, CourInfDO courInfDo);
    SubjectEntity ToSubjectEntity(SubjectDO subjectDO);
}
