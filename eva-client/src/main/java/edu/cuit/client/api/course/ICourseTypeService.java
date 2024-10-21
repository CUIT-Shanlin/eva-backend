package edu.cuit.client.api.course;

import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;

import java.util.List;

/**
 * 课程类型相关业务接口
 */
public interface ICourseTypeService {

    /**
     * 分页获取课程类型
     * @param  courseQuery 课程查询参数
     *
     * */
    PaginationQueryResultCO<CourseType> pageCourseType(PagingQuery<GenericConditionalQuery> courseQuery);

    /**
     * 获取所有的课程类型的信息
     * */
    List<CourseType> allCourseType();

    /**
     * 修改一节课的类型
     *  @param courseType 修改课课程类型
     *
     * */
    void updateCourseType(CourseType courseType);

    /**
     * 新建一个课程类型
     *
     *  @param courseType 课程类型
     *
     * */
    void addCourseType(CourseType courseType);

    /**
     * 删除一个课程类型
     *  @param id 课程详情id
     * */
    void deleteCourseType(Integer id);

    /**
     * 批量删除课程类型
     *  @param ids 课程数组
     * */
    void deleteCoursesType(List<Integer> ids);
}
