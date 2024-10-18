package edu.cuit.app.service.impl.course;

import edu.cuit.app.convertor.PaginationBizConvertor;

import edu.cuit.app.convertor.course.CourseBizConvertor;
import edu.cuit.client.api.course.ICourseTypeService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.course.CourseTypeEntity;
import edu.cuit.domain.gateway.course.CourseDeleteGateway;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Service
@RequiredArgsConstructor
public class ICourseTypeServiceImpl implements ICourseTypeService {
    private final CourseQueryGateway courseQueryGateway;
    private final CourseUpdateGateway courseUpdateGateway;
    private final CourseDeleteGateway courseDeleteGateway;
    private final CourseBizConvertor courseConvertor;
    private final PaginationBizConvertor pageConvertor;
    @Override
    public PaginationQueryResultCO<CourseType> pageCourseType(PagingQuery<GenericConditionalQuery> courseQuery) {
        PaginationResultEntity<CourseTypeEntity> entity = courseQueryGateway.pageCourseType(courseQuery);
        List<CourseType> list = entity.getRecords().stream().map(courseConvertor::toCourseType).toList();

        return pageConvertor.toPaginationEntity(entity,list);
    }

    @Override
    public List<CourseType> allCourseType() {
        PaginationResultEntity<CourseTypeEntity> entity = courseQueryGateway.pageCourseType(null);
        return entity.getRecords().stream().map(courseConvertor::toCourseType).toList();
    }

    @Override
    public void updateCourseType(CourseType courseType) {
        courseUpdateGateway.updateCourseType(courseType);
    }

    @Override
    public void addCourseType(CourseType courseType) {
        courseUpdateGateway.addCourseType(courseType);

    }

    @Override
    public void deleteCourseType(Integer id) {
        List<Integer> ids=null;
        if(id!=null){
            ids=new ArrayList<>();
            ids.add(id);
        }
        courseDeleteGateway.deleteCourseType(ids);
    }

    @Override
    public void deleteCoursesType(List<Integer> ids) {
        courseDeleteGateway.deleteCourseType(ids);

    }
}
