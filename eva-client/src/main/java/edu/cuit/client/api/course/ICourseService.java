package edu.cuit.client.api.course;

import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.course.*;
import edu.cuit.client.dto.cmd.course.AlignTeacherCmd;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import edu.cuit.client.dto.cmd.course.UpdateSingleCourseCmd;
import edu.cuit.client.dto.data.course.CoursePeriod;
import edu.cuit.client.dto.query.CourseQuery;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.CourseConditionalQuery;
import edu.cuit.client.dto.query.condition.MobileCourseQuery;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 一节课相关业务接口
 */
public interface ICourseService {
    /**
     * 获取周课表的课程数量
     *  @param semId 学期id
     *  @param week 哪一周?
     *
     * */
    List<List<Integer>> courseNum(Integer semId, Integer week);

    /**
     * 获取一个课程时间段的课程信息
     *  @param semId 学期id
     *  @param courseQuery 课程查询相关信息
     * */
    List<SingleCourseCO> courseTimeDetail(Integer semId, CourseQuery courseQuery);

    /**
     * 获取一节课的详细信息
     *@param semId 学期id
     *@param id 课程详情id
     * */
    SingleCourseDetailCO getCourseDetail(Integer semId,Integer id);

    /**
     * 获取一天的具体日期
     *@param semId 学期id
     *@param week 第几周
     *@param day 星期几
     * */
    String getDate(Integer semId,Integer week,Integer day);

    /**
     *获取某个指定时间段的课程
     * @param semId 学期id
     * @param courseQuery 课程查询条件
     */
    List<RecommendCourseCO> getTimeCourse(Integer semId, MobileCourseQuery courseQuery);

    /**
     * 修改一节课
     *@param semId 学期id
     *  @param updateSingleCourseCmd 修改单节课课程信息
     *
     * */
    void updateSingleCourse(Integer semId, UpdateSingleCourseCmd updateSingleCourseCmd);

    /**
     * 分配听课/评教老师
     *  @param semId 学期id
     *  @param alignTeacherCmd 内涵课程id，以及听课老师集合
     *
     * */
    void allocateTeacher(Integer semId, AlignTeacherCmd alignTeacherCmd);

    /**
     * 批量删除某节课
     *  @param semId 学期id
     *  @param id 对应课程编号
     *  @param coursePeriod 课程的一段时间模型
     * */
    void deleteCourses(Integer semId, Integer id, CoursePeriod coursePeriod );

    /**
     * 批量新建多节课(已有课程)
     *
     *  @param courseId 课程id
     *  @param timeCO 课程对应授课时间
     *
     * */
    void addExistCoursesDetails(Integer semId ,Integer courseId, SelfTeachCourseTimeCO timeCO);

    /**
     * 批量新建多节课(新课程)
     *  @param semId 学期ID
     *  @param teacherId 教学老师ID
     *  @param courseInfo 一门课程的可修改信息(一门课程的可修改信息)
     *  @param dateArr 自己教学的一门课程的一个课程时段模型集合
     * */
   void addNotExistCoursesDetails(Integer semId,Integer teacherId, UpdateCourseCmd courseInfo,  List<SelfTeachCourseTimeCO> dateArr);

}
