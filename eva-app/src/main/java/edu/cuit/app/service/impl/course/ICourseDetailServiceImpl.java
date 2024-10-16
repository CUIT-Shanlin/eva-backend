package edu.cuit.app.service.impl.course;

import edu.cuit.app.convertor.course.CourseConvertor;
import edu.cuit.client.api.course.ICourseDetailService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.SimpleCourseResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.course.CourseModelCO;
import edu.cuit.client.dto.clientobject.eva.CourseScoreCO;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import edu.cuit.client.dto.cmd.course.UpdateCoursesCmd;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.CourseConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.course.CourseEntity;
import edu.cuit.domain.entity.course.SubjectEntity;
import edu.cuit.domain.gateway.course.CourseDeleteGateway;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ICourseDetailServiceImpl implements ICourseDetailService {
    private final CourseQueryGateway courseQueryGateway;
    private final CourseUpdateGateway courseUpdateGateway;
    private final CourseDeleteGateway courseDeleteGateway;
    private final CourseConvertor courseConvertor;
    @Override
    public PaginationQueryResultCO<CourseModelCO> pageCoursesInfo(Integer semId, PagingQuery<CourseConditionalQuery> courseQuery) {

        return null;
    }

    @Override
    public CourseDetailCO courseInfo(Integer id, Integer semId) {

        Optional<CourseDetailCO> courseInfo = courseQueryGateway.getCourseInfo(id, semId);
        return courseInfo.get();
    }

    @Override
    public List<CourseScoreCO> evaResult(Integer id, Integer semId) {
        List<CourseScoreCO> evaScore = courseQueryGateway.findEvaScore(id, semId);
        return evaScore;
    }

    @Override
    public List<SimpleCourseResultCO> allCourseInfo(Integer semId) {
        PaginationResultEntity<CourseEntity> page = courseQueryGateway.page(null, semId);
        List<SimpleCourseResultCO> list = page.getRecords().stream().map(sim -> {
            return courseConvertor.toSimpleCourseResultCO(sim);
        }).toList();

        return list;
    }

    @Override
    public List<SimpleResultCO> allSubjectInfo() {
        List<SubjectEntity> subject = courseQueryGateway.findSubjectInfo();
        List<SimpleResultCO> list = subject.stream().map(subjectEntity -> courseConvertor.toSimpleResultCO(subjectEntity)).toList();
        return list;
    }

    @Override
    public void updateCourse(Integer semId, UpdateCourseCmd updateCourseCmd) {
            courseUpdateGateway.updateCourse(semId, updateCourseCmd);
    }

    @Override
    public void updateCourses(Integer semId, UpdateCoursesCmd updateCoursesCmd) {
      courseUpdateGateway.updateCourses(semId, updateCoursesCmd);
    }

    @Override
    public void addCourse(Integer semId) {
        courseUpdateGateway.addCourse(semId);
    }

    @Override
    public void delete(Integer semId, Integer id) {
        courseDeleteGateway.deleteCourse(semId, id);
    }
}
