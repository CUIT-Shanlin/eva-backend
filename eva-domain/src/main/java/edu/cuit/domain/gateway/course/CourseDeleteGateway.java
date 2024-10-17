package edu.cuit.domain.gateway.course;

import edu.cuit.client.dto.data.course.CoursePeriod;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 课程删除相关数据门户接口
 */
@Component
public interface CourseDeleteGateway {

    /**
     * 批量删除某节课
     *  @param semId 学期id
     *  @param id 对应课程编号
     *  @param coursePeriod 课程的一段时间模型
     * */
    Void deleteCourses(Integer semId, Integer id, CoursePeriod coursePeriod);

    /**
     * 连带删除一门课程
     *  @param semId 学期id
     *  @param id 对应课程编号
     * */
    Void deleteCourse(Integer semId,Integer id);

    /**
     * 删除一个课程类型/批量删除课程类型
     *   @param ids 课程类型数组
     * */
    Void deleteCourseType(List<Integer> ids);

    /**
     * 删除自己的一门课程
     *  @param courseId 课程id
     *  @param userName 用户名
     * */
    Void deleteSelfCourse(String userName,Integer courseId);
}
