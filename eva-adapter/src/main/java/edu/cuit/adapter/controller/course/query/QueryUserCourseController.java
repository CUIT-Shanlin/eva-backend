package edu.cuit.adapter.controller.course.query;

import edu.cuit.client.dto.query.condition.CourseConditionalQuery;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户课程信息查询相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class QueryUserCourseController {

    /**
     * 获取单个用户教学的课程基础信息
     *@param semId 学期id
     *  @param id 用户编号id
     *
     * */
    @GetMapping("/courses")
    public CommonResult<Object> getUserCourseInfo(@RequestParam("id") Integer id,
                                                  @RequestParam("semId") Integer semId){
        return null;
    }

    /**
     * 获取单个用户的教学课程的详细信息
     *@param semId 学期id
     *  @param id 用户编号id
     *
     * */
    @GetMapping("/courses/detail")
    public CommonResult<Object> getUserCourseDetail(@RequestParam("id") Integer id,
                                                    @RequestParam("semId") Integer semId){
        return null;
    }

    /**
     *获取自己的推荐选课
     * @param semId 学期id
     */
    @GetMapping("/courses/suggestion")
    public CommonResult<Object> getSelfCourse(@RequestParam("semId") Integer semId){
        return null;
    }

    /**
     *获取某个指定时间段的课程
     * @param semId 学期id
     * @param courseQuery 课程查询条件
     */
    @PostMapping("/courses/query")
    public CommonResult<Object> getTimeCourse(@RequestParam("semId") Integer semId,
                                              @RequestBody CourseConditionalQuery courseQuery){
        return null;
    }
}
