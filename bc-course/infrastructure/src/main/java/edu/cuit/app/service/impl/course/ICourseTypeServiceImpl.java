package edu.cuit.app.service.impl.course;

import edu.cuit.bc.course.application.usecase.CourseTypeUseCase;
import edu.cuit.client.api.course.ICourseTypeService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.cmd.course.UpdateCourseTypeCmd;
import edu.cuit.client.dto.cmd.course.UpdateCoursesToTypeCmd;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ICourseTypeServiceImpl implements ICourseTypeService {
    private final CourseTypeUseCase courseTypeUseCase;

    @Override
    public PaginationQueryResultCO<CourseType> pageCourseType(PagingQuery<GenericConditionalQuery> courseQuery) {
        return courseTypeUseCase.pageCourseType(courseQuery);
    }

    @Override
    public List<CourseType> allCourseType() {
        return courseTypeUseCase.allCourseType();
    }

    @Override
    public void updateCourseType(UpdateCourseTypeCmd courseType) {
        courseTypeUseCase.updateCourseType(courseType);
    }

    @Override
    public void addCourseType(CourseType courseType) {
        courseTypeUseCase.addCourseType(courseType);
    }

    @Override
    public void deleteCourseType(Integer id) {
        courseTypeUseCase.deleteCourseType(id);
    }

    @Override
    public void deleteCoursesType(List<Integer> ids) {
        courseTypeUseCase.deleteCoursesType(ids);
    }

    @Override
    public Void updateCoursesType(UpdateCoursesToTypeCmd updateCoursesToTypeCmd) {
        courseTypeUseCase.updateCoursesType(updateCoursesToTypeCmd);
        return null;
    }
}
