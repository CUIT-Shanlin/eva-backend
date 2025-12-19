package edu.cuit.infra.gateway.impl.course;

import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.course.RecommendCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SingleCourseCO;
import edu.cuit.client.dto.clientobject.eva.CourseScoreCO;
import edu.cuit.client.dto.clientobject.eva.EvaTeacherInfoCO;
import edu.cuit.client.dto.data.course.CourseTime;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.client.dto.query.CourseQuery;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.CourseConditionalQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.client.dto.query.condition.MobileCourseQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.course.CourseEntity;
import edu.cuit.domain.entity.course.CourseTypeEntity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.course.SubjectEntity;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.infra.bccourse.query.CourseQueryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 课程查询 gateway（读侧入口）。
 *
 * <p>说明：复杂查询与组装逻辑已抽取到 {@link CourseQueryRepo}，本类退化为委托壳，便于后续进一步模块化与 CQRS 演进。</p>
 */
@Component
@RequiredArgsConstructor
public class CourseQueryGatewayImpl implements CourseQueryGateway {
    private final CourseQueryRepo repo;

    @Override
    public PaginationResultEntity<CourseEntity> page(PagingQuery<CourseConditionalQuery> courseQuery, Integer semId) {
        return repo.page(courseQuery, semId);
    }

    @Override
    public Optional<CourseDetailCO> getCourseInfo(Integer id, Integer semId) {
        return repo.getCourseInfo(id, semId);
    }

    @Override
    public List<CourseScoreCO> findEvaScore(Integer id) {
        return repo.findEvaScore(id);
    }

    @Override
    public List<SubjectEntity> findSubjectInfo() {
        return repo.findSubjectInfo();
    }

    @Override
    public List<List<Integer>> getWeekCourses(Integer semId, Integer week) {
        return repo.getWeekCourses(semId, week);
    }

    @Override
    public List<SingleCourseCO> getPeriodInfo(Integer semId, CourseQuery courseQuery) {
        return repo.getPeriodInfo(semId, courseQuery);
    }

    @Override
    public Optional<SingleCourseEntity> getSingleCourseDetail(Integer id, Integer semId) {
        return repo.getSingleCourseDetail(id, semId);
    }

    @Override
    public List<RecommendCourseCO> getPeriodCourse(Integer semId, MobileCourseQuery courseQuery, String userName) {
        return repo.getPeriodCourse(semId, courseQuery, userName);
    }

    @Override
    public PaginationResultEntity<CourseTypeEntity> pageCourseType(PagingQuery<GenericConditionalQuery> courseQuery) {
        return repo.pageCourseType(courseQuery);
    }

    @Override
    public List<List<SingleCourseEntity>> getUserCourseDetail(Integer id, Integer semId) {
        return repo.getUserCourseDetail(id, semId);
    }

    @Override
    public List<SelfTeachCourseCO> getSelfCourseInfo(String userName, Integer semId) {
        return repo.getSelfCourseInfo(userName, semId);
    }

    @Override
    public List<RecommendCourseCO> getSelfCourse(Integer semId, String userName) {
        return repo.getSelfCourse(semId, userName);
    }

    @Override
    public List<SingleCourseEntity> getSelfCourseTime(Integer id) {
        return repo.getSelfCourseTime(id);
    }

    @Override
    public String getDate(Integer semId, Integer week, Integer day) {
        return repo.getDate(semId, week, day);
    }

    @Override
    public List<String> getLocation(Integer courseId) {
        return repo.getLocation(courseId);
    }

    @Override
    public Optional<CourseEntity> getCourseByInfo(Integer courInfId) {
        return repo.getCourseByInfo(courInfId);
    }

    @Override
    public Optional<CourseTime> getCourseTimeByCourse(Integer courInfId) {
        return repo.getCourseTimeByCourse(courInfId);
    }

    @Override
    public List<CourseType> getCourseType(Integer courseId) {
        return repo.getCourseType(courseId);
    }

    @Override
    public List<EvaTeacherInfoCO> getEvaUsers(Integer courseId) {
        return repo.getEvaUsers(courseId);
    }

    @Override
    public List<Integer> getUserCourses(Integer semId, Integer userId) {
        return repo.getUserCourses(semId, userId);
    }
}

