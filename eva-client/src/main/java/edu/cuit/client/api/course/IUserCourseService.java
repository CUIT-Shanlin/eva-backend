package edu.cuit.client.api.course;

import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.course.ModifySingleCourseDetailCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import jakarta.validation.Valid;


import java.io.InputStream;
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

    /**
     * 导入课表文件
     *
     *  @param fileStream 课表文件
     *
     * */
    void importCourse(InputStream fileStream);

    /**
     * 获取自己所有教学的课程的详细信息
     * @param semId 学期id
     * */
    List<Void> selfCourseDetail(Integer semId);

    /**
     * 获取自己所有教学的课程的详细信息
     * @param courseId 课程id
     * */
    List<SelfTeachCourseTimeCO> selfCourseTime(Integer courseId);

    /**
     * 删除自己的一门课程
     *  @param courseId 课程id
     * */
    Void deleteSelfCourse(Integer courseId);

    /**
     * 修改自己的一门课程信息及其课程时段
     *@param selfTeachCourseCO 用于确定是导入实验课表还是理论课表，0：理论课，1：实验课
     *  @param timeList 课表文件
     * */
   Void updateSelfCourse(SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeCO> timeList);



}
