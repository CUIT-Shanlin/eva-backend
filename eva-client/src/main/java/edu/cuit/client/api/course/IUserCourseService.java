package edu.cuit.client.api.course;

import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.course.ModifySingleCourseDetailCO;

import java.util.List;

/**
 * 用户课程业务接口
 */
public interface IUserCourseService {

    /**
     * 获取单个用户教学的课程基础信息
     *  @param semId 学期id
     *  @param id 用户编号id
     * */
    List<SimpleResultCO> getUserCourseInfo(Integer id, Integer semId);

    /**
     * 获取单个用户的教学课程的详细信息
     *  @param semId 学期id
     *  @param id 用户编号id
     * */
    List<CourseDetailCO> getUserCourseDetail(Integer id, Integer semId);

    /**
     * 获取自己的推荐选课
     * @param semId 学期id
     */
    List<ModifySingleCourseDetailCO> getSelfCourse(Integer semId);

}
