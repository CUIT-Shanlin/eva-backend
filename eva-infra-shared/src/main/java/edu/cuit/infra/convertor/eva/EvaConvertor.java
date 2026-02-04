package edu.cuit.infra.convertor.eva;

import edu.cuit.client.dto.clientobject.eva.EvaTaskFormCO;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.domain.entity.course.CourseEntity;
import edu.cuit.domain.entity.course.SemesterEntity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.eva.CourOneEvaTemplateEntity;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
import edu.cuit.domain.entity.eva.EvaTemplateEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.infra.convertor.EntityFactory;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.function.Supplier;

@Mapper(componentModel = "spring",uses = EntityFactory.class)
public interface EvaConvertor {
    @Mappings({
            @Mapping(target = "id",source = "courOneEvaTemplateDO.id"),
            @Mapping(target = "semester",source = "semester"),
            @Mapping(target = "course",source = "course"),
            @Mapping(target = "formTemplate",source = "courOneEvaTemplateDO.formTemplate"),
            @Mapping(target = "courseStatistics",source = "courOneEvaTemplateDO.courseStatistics")
    })
    CourOneEvaTemplateEntity ToCourOneEvaTemplateEntity(CourOneEvaTemplateDO courOneEvaTemplateDO, Supplier<SemesterEntity> semester, Supplier<CourseEntity> course);
    @Mappings({
            @Mapping(target = "id",source = "evaTaskDO.id"),
            @Mapping(target = "teacher",source = "teacher"),
            @Mapping(target = "courInf",source = "courInf"),
            @Mapping(target = "status",source = "evaTaskDO.status"),
            @Mapping(target = "createTime",source = "evaTaskDO.createTime"),
            @Mapping(target = "updateTime",source = "evaTaskDO.updateTime"),
            @Mapping(target = "isDeleted",source = "evaTaskDO.isDeleted")
    })
    EvaTaskEntity ToEvaTaskEntity(EvaTaskDO evaTaskDO,Supplier<UserEntity> teacher, Supplier<SingleCourseEntity> courInf);
    @Mappings({
            @Mapping(target = "id",source = "formRecordDO.id"),
            @Mapping(target = "task",source = "task"),
            @Mapping(target = "textValue",source = "formRecordDO.textValue"),
            @Mapping(target = "formPropsValues",source = "formRecordDO.formPropsValues"),
            @Mapping(target = "createTime",source = "formRecordDO.createTime"),
            @Mapping(target = "isDeleted",source = "formRecordDO.isDeleted")
    })
    EvaRecordEntity ToEvaRecordEntity(FormRecordDO formRecordDO, Supplier<EvaTaskEntity> task);
    @Mappings({
            @Mapping(target = "isDefault",source = "formTemplateDO.isDefault"),
    })
    EvaTemplateEntity ToEvaTemplateEntity(FormTemplateDO formTemplateDO);


    /**
     * 过渡期桥接方法：用于让调用方在不编译期引用 {@link UserEntity} 的情况下复用既有映射逻辑。
     * <p>
     * 约束：不改变 teacher/courInf Supplier 的调用时机与次数，仅做类型桥接。
     * </p>
     */
    default EvaTaskEntity toEvaTaskEntityWithTeacherObject(
            EvaTaskDO evaTaskDO, Supplier<?> teacher, Supplier<SingleCourseEntity> courInf) {
        return ToEvaTaskEntity(evaTaskDO, () -> (UserEntity) teacher.get(), courInf);
    }


}
