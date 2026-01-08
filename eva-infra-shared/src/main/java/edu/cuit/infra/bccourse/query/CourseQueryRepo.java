package edu.cuit.infra.bccourse.query;

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

import java.util.List;
import java.util.Optional;

/**
 * 课程读侧 QueryRepo（将复杂查询与组装从 gateway 中抽离）。
 *
 * <p>注意：此处是“渐进式结构化”的第一步，暂不引入 CQRS 投影表，保持行为不变。</p>
 */
public interface CourseQueryRepo {
    PaginationResultEntity<CourseEntity> page(PagingQuery<CourseConditionalQuery> courseQuery, Integer semId);

    Optional<CourseDetailCO> getCourseInfo(Integer id, Integer semId);

    List<CourseScoreCO> findEvaScore(Integer id);

    List<SubjectEntity> findSubjectInfo();

    List<List<Integer>> getWeekCourses(Integer semId, Integer week);

    List<SingleCourseCO> getPeriodInfo(Integer semId, CourseQuery courseQuery);

    Optional<SingleCourseEntity> getSingleCourseDetail(Integer id, Integer semId);

    List<RecommendCourseCO> getPeriodCourse(Integer semId, MobileCourseQuery courseQuery, String userName);

    PaginationResultEntity<CourseTypeEntity> pageCourseType(PagingQuery<GenericConditionalQuery> courseQuery);

    List<List<SingleCourseEntity>> getUserCourseDetail(Integer id, Integer semId);

    List<SelfTeachCourseCO> getSelfCourseInfo(String userName, Integer semId);

    List<RecommendCourseCO> getSelfCourse(Integer semId, String userName);

    List<SingleCourseEntity> getSelfCourseTime(Integer id);

    String getDate(Integer semId, Integer week, Integer day);

    List<String> getLocation(Integer courseId);

    List<CourseType> getCourseType(Integer courseId);

    List<EvaTeacherInfoCO> getEvaUsers(Integer courseId);

    List<Integer> getUserCourses(Integer semId, Integer userId);

    Optional<CourseEntity> getCourseByInfo(Integer courInfId);

    Optional<CourseTime> getCourseTimeByCourse(Integer courInfId);
}

