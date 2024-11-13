package edu.cuit.adapter.controller.course.query;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.app.service.impl.course.ICourseDetailServiceImpl;
import edu.cuit.app.service.impl.course.ICourseServiceImpl;
import edu.cuit.app.service.impl.course.ICourseTypeServiceImpl;
import edu.cuit.app.service.impl.course.IUserCourseServiceImpl;
import edu.cuit.client.api.course.ICourseDetailService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.SimpleCourseResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.SimpleSubjectResultCO;
import edu.cuit.client.dto.clientobject.course.*;
import edu.cuit.client.dto.clientobject.eva.CourseScoreCO;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.client.dto.query.CourseQuery;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.CourseConditionalQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.client.dto.query.condition.MobileCourseQuery;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;


/**
 * 课程信息查询相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class QueryCourseController {
    private final ICourseDetailServiceImpl courseDetailService;
    private final ICourseServiceImpl courseService;
    private final ICourseTypeServiceImpl courseTypeService;
    private final IUserCourseServiceImpl userCourseService;
    /**
     * 分页获取课程列表
     *
     * @param semId 学期id
     * @param queryObj 课程查询参数
     */
    @PostMapping("/courses")
    @SaCheckPermission("course.tabulation.query")
    public CommonResult<PaginationQueryResultCO<CourseModelCO>> pageCoursesInfo(
            @RequestParam(value = "semId",required = false) Integer semId,
            @Valid @RequestBody PagingQuery<CourseConditionalQuery> queryObj){
        return CommonResult.success(courseDetailService.pageCoursesInfo(semId, queryObj));

    }

    /**
     * 获取一门课程的信息
     *
     * @param semId 学期id
     * @param id ID编号
     */
    @GetMapping("/course")
    @SaCheckPermission("course.tabulation.query")
    public CommonResult<CourseDetailCO> courseInfo(
            @RequestParam(value = "id",required = true) Integer id,
            @RequestParam(value = "semId",required = false) Integer semId){
        return CommonResult.success(courseDetailService.courseInfo(id, semId));
    }

    /**
     * 一门课程的评教统计
     * @param id ID编号
     */
    @GetMapping("/course/eva")
    @SaCheckPermission("course.tabulation.eva.query")
    public CommonResult<List<CourseScoreCO>> evaResult(
            @RequestParam(value = "id",required = true) Integer id){
        return CommonResult.success(courseDetailService.evaResult(id));
    }

    /**
     * 获取所有的课程的基础信息
     *
     * @param semId 学期id
     *
     */
    @GetMapping("/courses/all")
    @SaCheckPermission("course.tabulation.list")
    public CommonResult<List<SimpleCourseResultCO>> allCourseInfo(
            @RequestParam(value = "semId",required = false) Integer semId){
       return CommonResult.success(courseDetailService.allCourseInfo(semId));
    }

    /**
     * 获取所有的科目的基础信息
     */
    @GetMapping("/courses/subject/all")
    @SaCheckPermission("course.tabulation.list")
    public CommonResult<List<SimpleSubjectResultCO>> allSubjectInfo(){
        return CommonResult.success(courseDetailService.allSubjectInfo());
    }

    /**
     * 获取周课表的课程数量
     *  @param semId 学期id
     *  @param week 哪一周?
     *
     * */
    @GetMapping("/courses/table")
    @SaCheckPermission("course.table.amount")
    public CommonResult<List<List<Integer>>> courseNum(
            @RequestParam(value = "week",required = true) Integer week,
            @RequestParam(value = "semId",required = false) Integer semId){
       return CommonResult.success(courseService.courseNum(week, semId));
    }

    /**
     * 获取一个课程时间段的课程信息
     *  @param semId 学期id
     *  @param courseQuery 课程查询相关信息
     * */
    @PostMapping("/course/table")
    @SaCheckPermission("course.table.query")
    public CommonResult<List<SingleCourseCO>> courseTimeDetail(
            @RequestParam(value = "semId",required = false) Integer semId,
            @Valid @RequestBody CourseQuery courseQuery){
        return CommonResult.success(courseService.courseTimeDetail(semId, courseQuery));
    }

    /**
     * 获取一节课的详细信息
     *@param semId 学期id
     *@param id 课程详情id
     * */
    @GetMapping("/course/table/one")
    @SaCheckPermission("course.table.query")
    public CommonResult<SingleCourseDetailCO> getCourseDetail(
            @RequestParam(value = "id",required = true) Integer id,
            @RequestParam(value = "semId",required = false) Integer semId){
       return CommonResult.success(courseService.getCourseDetail(semId, id));
    }

    /**
     * 获取一天的具体日期
     *@param semId 学期id
     *@param week 第几周
     *@param day 星期几
     * */
    @GetMapping("/course/date")
    @SaCheckLogin
    public CommonResult<String> getDate(
            @RequestParam(value = "semId",required = false) Integer semId,
            @RequestParam(value = "week",required = true) Integer week,
            @RequestParam(value = "day",required = true) Integer day){
        return CommonResult.success(courseService.getDate(semId, week, day));
    }

    /**
     *获取某个指定时间段的课程
     * @param semId 学期id
     * @param courseQuery 课程查询条件
     */
    @PostMapping("/courses/query")
    @SaCheckLogin
    public CommonResult<List<RecommendCourseCO>> getTimeCourse(
            @RequestParam(value = "semId",required = false) Integer semId,
             @RequestBody MobileCourseQuery courseQuery){
        return CommonResult.success(courseService.getTimeCourse(semId, courseQuery));
    }

    /**
     * 分页获取课程类型
     * @param  courseQuery 课程查询参数
     *
     * */
    @PostMapping("/course/types")
    @SaCheckPermission("course.type.query")
    public CommonResult<PaginationQueryResultCO<CourseType>> pageCourseType(
            @Valid @RequestBody PagingQuery<GenericConditionalQuery> courseQuery){
        return CommonResult.success(courseTypeService.pageCourseType(courseQuery));
    }

    /**
     * 获取所有的课程类型的信息
     * */
    @GetMapping("/courses/types/all")
    @SaCheckPermission("course.type.query")
    public CommonResult<List<CourseType>> allCourseType(){
        return CommonResult.success(courseTypeService.allCourseType());
    }


}
