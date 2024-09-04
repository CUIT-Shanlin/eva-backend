package edu.cuit.adapter.controller.course.query;

import edu.cuit.client.dto.data.course.CourseTime;
import edu.cuit.client.dto.query.condition.CourseConditionalQuery;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


/**
 * 课程信息查询相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class QueryCourseController {
    /**
     * 分页获取课程列表
     *
     * @param semId 学期id
     * @param courseQuery 课程查询参数
     */
    @PostMapping("/courses")
    public CommonResult<Object> pageCoursesInfo(@RequestParam("semId") Integer semId,
                                                @RequestBody CourseConditionalQuery courseQuery){
        return null;
    }

    /**
     * 获取一门课程的信息
     *
     * @param semId 学期id
     * @param id ID编号
     */
    @GetMapping("/course")
    public CommonResult<Object> courseinfo(@RequestParam("id") Integer id,
                                           @RequestParam("semId") Integer semId){
        return null;
    }

    /**
     * 一门课程的评教统计
     *
     * @param semId 学期id
     * @param id ID编号
     */
    @GetMapping("/course/eva")
    public CommonResult<Object> evaResult(@RequestParam("id") Integer id,
                                          @RequestParam("semId") Integer semId){
        return null;
    }

    /**
     * 获取所有的课程的基础信息
     *
     * @param semId 学期id
     *
     */
    @GetMapping("/courses/all")
    public CommonResult<Object> allCourseInfo(@RequestParam("semId") Integer semId){
        return null;
    }

    /**
     * 获取周课表的课程数量
     *@param semId 学期id
     *  @param week 哪一周?
     *
     * */
    @GetMapping("/courses/table")
    public CommonResult<Object> courseNum(@RequestParam("week") Integer week,
                                          @RequestParam("semId") Integer semId){
        return null;
    }

    /**
     * 获取一个课程时间段的课程信息
     *@param semId 学期id
     *  @param body 含有课程时间课程时间（需要自己提取出来）
     * Object param = body.get("想要获取的参数名称");
     *返回类型与json中的保持一致
     * */
    //TODO
    @PostMapping("/course/table")
    public CommonResult<Object> courseTimeDetail(@RequestParam("semId") Integer semId,
                                                 @RequestBody Map<String, Object> body){
        return null;
    }

    /**
     * 获取一节课的详细信息
     *@param semId 学期id
     *  @param id 课程详情id
     *
     * */
    @GetMapping("/course/table/one")
    public CommonResult<Object> getCourseDetail(@RequestParam("id") Integer id,
                                                @RequestParam("semId") Integer semId){
        return null;
    }

    /**
     * 分页获取课程类型
     *@param  courseQuery 课程查询参数
     *
     * */
    @PostMapping("/course/types")
    public CommonResult<Object> pageCourseType(@RequestBody CourseConditionalQuery courseQuery){
        return null;
    }

    /**
     * 获取所有的课程类型的信息
     *
     * */
    @GetMapping("/courses/types/all")
    public CommonResult<Object> allCourseType(){
        return null;
    }
}
