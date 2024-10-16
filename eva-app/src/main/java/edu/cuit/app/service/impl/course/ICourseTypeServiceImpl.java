package edu.cuit.app.service.impl.course;

import edu.cuit.client.api.course.ICourseTypeService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class ICourseTypeServiceImpl implements ICourseTypeService {
    @Override
    public PaginationQueryResultCO<CourseType> pageCourseType(PagingQuery<GenericConditionalQuery> courseQuery) {
        return null;
    }

    @Override
    public List<CourseType> allCourseType() {
        return null;
    }

    @Override
    public void updateCourseType(CourseType courseType) {

    }

    @Override
    public void addCourseType(CourseType courseType) {

    }

    @Override
    public void deleteCourseType(Integer id) {

    }

    @Override
    public void deleteCoursesType(List<Integer> ids) {

    }
}
