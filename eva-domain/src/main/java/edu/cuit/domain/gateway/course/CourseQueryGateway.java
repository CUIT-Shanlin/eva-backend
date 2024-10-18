package edu.cuit.domain.gateway.course;


import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.course.RecommendCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SingleCourseCO;

import edu.cuit.client.dto.clientobject.eva.CourseScoreCO;

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
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 课程查询相关数据门户接口
 */
@Component
public interface CourseQueryGateway {
    /**
     * 分页获取课程列表/获取所有的课程的基础信息
     *@param semId 学期id
     * @param courseQuery 课程查询参数
     * @return List<CourseEntity>
     */
    PaginationResultEntity<CourseEntity> page(PagingQuery<CourseConditionalQuery> courseQuery, Integer semId);

    /**
     * 获取一门课程的信息
     *
     * @param semId 学期id
     * @param id ID编号
     *@return  Optional<CourseDetailCO>
     */
    Optional<CourseDetailCO> getCourseInfo(Integer id, Integer semId);

    /**
     * 获取一门课程的评教统计
     *@param semId 学期id
     * @param id 课程编号id
     * @return List<CourseScoreCO>
     */
    List<CourseScoreCO> findEvaScore(Integer id,Integer semId);

    /**
     * 获取所有的科目的基础信息
     * @return  List<SubjectEntity>
     */
    List<SubjectEntity> findSubjectInfo();

    /**
     * 获取周课表的课程数量
     *  @param semId 学期id
     *  @param week 哪一周?
     *  @return List<List<Integer>>
     * */
    List<List<Integer>> getWeekCourses(Integer week,Integer semId);

    /**
     * 获取一个课程时间段的课程信息
     *  @param semId 学期id
     *  @param courseQuery 课程查询相关信息
     * @return List<SingleCourseCO>
     * */
    List<SingleCourseCO> getPeriodInfo(Integer semId,CourseQuery courseQuery);

    /**
     * 获取一节课的详细信息
     *@param semId 学期id
     *@param id 课程详情id
     * @return Optional<SingleCourseEntity>
     * */
    Optional<SingleCourseEntity> getSingleCourseDetail(Integer id,Integer semId);

    /**
     *获取某个指定时间段的课程
     * @param semId 学期id
     * @param courseQuery 课程查询条件
     *@return List<SingleCourseEntity>
     */
    List<RecommendCourseCO> getPeriodCourse(Integer semId, MobileCourseQuery courseQuery,String userName);

    /**
     * 分页获取课程类型
     * @param  courseQuery 课程查询参数
     * @return List<CourseTypeEntity>
     * */
    PaginationResultEntity<CourseTypeEntity> pageCourseType(PagingQuery<GenericConditionalQuery> courseQuery);

    /**
     * 获取单个用户的教学课程的详细信息
     *  @param semId 学期id
     *  @param id 用户编号id
     * @return List<SingleCourseEntity>
     * */
    List<SingleCourseEntity> getUserCourseDetail( Integer id,Integer semId);

    /**
     * 获取自己教学的课程基础信息/获取自己所有教学的课程的详细信息
     *  @param semId 学期id
     *  @param userName 用户名
     * @return List<SelfTeachCourseCO>
     * */
    List<SelfTeachCourseCO> getSelfCourseInfo(String userName, Integer semId);

    /**
     * 获取自己的推荐选课
     * @param semId 学期id
     * @return List<CourseEntity>
     * */
    List<RecommendCourseCO> getSelfCourse(Integer semId, String userName);

    /**
     * 获取自己教学的一门课程的课程时段
     * @param id 课程id
     * @return List<SingleCourseEntity>
     * */
    List<SingleCourseEntity> getSelfCourseTime(Integer id);

    /**
     * 获取一天的具体日期
     *@param semId 学期id
     *@param week 第几周
     *@param day 星期几
     * */
    String getDate(Integer semId,Integer week,Integer day);

    /**
     * 获取课程教师位置
     *@param courseId 课程id
     * */
    List<String> getLocation(Integer courseId);
    /**
     * 根据cousInfId 获得课程对象
     * @param courInfId
     */
    Optional<CourseEntity> getCourseByInfo(Integer courInfId);
    /**
     * 通过一节课得到courseTime
     * @param courInfId
     */
    Optional<CourseTime> getCourseTimeByCourse(Integer courInfId);

    /**
     * 根据课程id来获取课程类型集合
     *@param courseId 课程id
     * */
    List<CourseType> getCourseType(Integer courseId);

    /**
     * 根据一节课程的id来获取评教该节课的老师集合
     *@param courseId 该节课的id
     * */
    List<EvaTeacherInfoCO> getEvaUsers(Integer courseId);




}
