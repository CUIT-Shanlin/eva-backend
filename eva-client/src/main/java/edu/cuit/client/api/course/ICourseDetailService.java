package edu.cuit.client.api.course;

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
import jakarta.validation.Valid;

import java.util.List;

/**
 * 一门课相关业务接口
 */
public interface ICourseDetailService {

    /**
     * 分页获取课程列表
     *
     * @param semId 学期id
     * @param courseQuery 课程查询参数
     */
    PaginationQueryResultCO<CourseModelCO> pageCoursesInfo(Integer semId,PagingQuery<CourseConditionalQuery> courseQuery);

    /**
     * 获取一门课程的信息
     *
     * @param semId 学期id
     * @param id ID编号
     */
    CourseDetailCO courseInfo(Integer id,Integer semId);

    /**
     * 一门课程的评教统计
     *
     * @param id ID编号
     */
    List<CourseScoreCO> evaResult(Integer id);

    /**
     * 获取所有的课程的基础信息
     *
     * @param semId 学期id
     *
     */
    List<SimpleCourseResultCO> allCourseInfo(Integer semId);

    /**
     * 获取所有的科目的基础信息
     *
     *
     */
    List<SimpleResultCO> allSubjectInfo();

    /**
     * 修改一门课程
     *  @param semId 学期id
     *  @param updateCourseCmd 修改课程信息
     *
     * */
    void updateCourse(Integer semId, UpdateCourseCmd updateCourseCmd);

    /**
     * 批量修改课程的模板
     *@param semId 学期id
     *  @param updateCoursesCmd 批量修改课程信息
     *
     * */
    void updateCourses(Integer semId, UpdateCoursesCmd updateCoursesCmd);

    /**
     * 新建一门课程
     *  @param semId 学期id
     *
     * */
    void addCourse(Integer semId);

    /**
     * 连带删除一门课程
     *  @param semId 学期id
     *  @param id 对应课程编号
     * */
    void delete(Integer semId,Integer id);



}
