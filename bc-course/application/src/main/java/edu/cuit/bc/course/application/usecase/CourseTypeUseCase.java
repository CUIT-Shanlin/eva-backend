package edu.cuit.bc.course.application.usecase;

import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.cmd.course.UpdateCourseTypeCmd;
import edu.cuit.client.dto.cmd.course.UpdateCoursesToTypeCmd;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.course.CourseTypeEntity;
import edu.cuit.domain.gateway.course.CourseDeleteGateway;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 课程类型用例（读写合并，保持行为不变）。
 *
 * <p>说明：本用例用于承接 {@code eva-app} 中旧入口 {@code ICourseTypeServiceImpl} 的业务编排；
 * 现阶段仅做结构性重构，不引入额外依赖，不改变异常文案/副作用顺序/返回语义。</p>
 */
public class CourseTypeUseCase {
    private final CourseQueryGateway courseQueryGateway;
    private final CourseUpdateGateway courseUpdateGateway;
    private final CourseDeleteGateway courseDeleteGateway;

    public CourseTypeUseCase(
            CourseQueryGateway courseQueryGateway,
            CourseUpdateGateway courseUpdateGateway,
            CourseDeleteGateway courseDeleteGateway
    ) {
        this.courseQueryGateway = Objects.requireNonNull(courseQueryGateway, "courseQueryGateway");
        this.courseUpdateGateway = Objects.requireNonNull(courseUpdateGateway, "courseUpdateGateway");
        this.courseDeleteGateway = Objects.requireNonNull(courseDeleteGateway, "courseDeleteGateway");
    }

    public PaginationQueryResultCO<CourseType> pageCourseType(PagingQuery<GenericConditionalQuery> courseQuery) {
        PaginationResultEntity<CourseTypeEntity> entity = courseQueryGateway.pageCourseType(courseQuery);
        List<CourseType> list = entity.getRecords().stream().map(this::toCourseType).toList();
        return toPaginationEntity(entity, list);
    }

    public List<CourseType> allCourseType() {
        PaginationResultEntity<CourseTypeEntity> entity = courseQueryGateway.pageCourseType(null);
        return entity.getRecords().stream().map(this::toCourseType).toList();
    }

    public void updateCourseType(UpdateCourseTypeCmd courseType) {
        courseUpdateGateway.updateCourseType(courseType);
    }

    public void addCourseType(CourseType courseType) {
        courseUpdateGateway.addCourseType(courseType);
    }

    public void deleteCourseType(Integer id) {
        List<Integer> ids = null;
        if (id != null) {
            ids = new ArrayList<>();
            ids.add(id);
        }
        courseDeleteGateway.deleteCourseType(ids);
    }

    public void deleteCoursesType(List<Integer> ids) {
        courseDeleteGateway.deleteCourseType(ids);
    }

    public void updateCoursesType(UpdateCoursesToTypeCmd updateCoursesToTypeCmd) {
        courseUpdateGateway.updateCoursesType(updateCoursesToTypeCmd);
    }

    private CourseType toCourseType(CourseTypeEntity courseTypeEntity) {
        return new CourseType()
                .setId(courseTypeEntity.getId())
                .setName(courseTypeEntity.getName())
                .setDescription(courseTypeEntity.getDescription())
                .setCreateTime(courseTypeEntity.getCreateTime())
                .setUpdateTime(courseTypeEntity.getUpdateTime())
                .setIsDefault(courseTypeEntity.getIsDefault());
    }

    private <T> PaginationQueryResultCO<T> toPaginationEntity(PaginationResultEntity<?> paginationResultEntity, List<T> values) {
        PaginationQueryResultCO<T> pageCO = new PaginationQueryResultCO<>();
        pageCO.setCurrent(paginationResultEntity.getCurrent())
                .setSize(paginationResultEntity.getSize())
                .setTotal(paginationResultEntity.getTotal())
                .setRecords(values);
        return pageCO;
    }
}
