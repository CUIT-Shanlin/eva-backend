package edu.cuit.adapter.controller.course.query;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.course.*;
import edu.cuit.client.dto.clientobject.eva.CourseScoreCO;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.client.dto.query.CourseQuery;
import edu.cuit.client.dto.query.condition.CourseConditionalQuery;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;


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
    @SaCheckPermission("course.tabulation.query")
    public CommonResult<PaginationQueryResultCO<CourseModuleCO>> pageCoursesInfo(
            @RequestParam(value = "semId",required = false) Integer semId,
            @Valid @RequestBody CourseConditionalQuery courseQuery){
        return null;
    }

    /**
     * 获取一门课程的信息
     *
     * @param semId 学期id
     * @param id ID编号
     */
    @GetMapping("/course")
    @SaCheckPermission("course.tabulation.query")
    public CommonResult<CourseDetailCO> courseinfo(
            @RequestParam(value = "id",required = false) Integer id,
            @RequestParam(value = "semId",required = false) Integer semId){
        return null;
    }

    /**
     * 一门课程的评教统计
     *
     * @param semId 学期id
     * @param id ID编号
     */
    @GetMapping("/course/eva")
    @SaCheckPermission("course.tabulation.eva.query")
    public CommonResult<CourseScoreCO> evaResult(
            @RequestParam(value = "id",required = false) Integer id,
            @RequestParam(value = "semId",required = false) Integer semId){
        return null;
    }

    /**
     * 获取所有的课程的基础信息
     *
     * @param semId 学期id
     *
     */
    @GetMapping("/courses/all")
    @SaCheckPermission("course.tabulation.list")
    public CommonResult<SimpleResultCO> allCourseInfo(
            @RequestParam(value = "semId",required = false) Integer semId){
        return null;
    }

    /**
     * 获取周课表的课程数量
     *@param semId 学期id
     *  @param week 哪一周?
     *
     * */
    @GetMapping("/courses/table")
    @SaCheckPermission("course.table.amount")
    public CommonResult<CourseComonCO<ArrayList<Integer>>> courseNum(
            @RequestParam(value = "week",required = false) Integer week,
            @RequestParam(value = "semId",required = false) Integer semId){
        return null;
    }

    /**
     * 获取一个课程时间段的课程信息
     *@param semId 学期id
     *  @param courseQuery 课程查询相关信息
     * */
    //TODO
    @PostMapping("/course/table")
    @SaCheckPermission("course.table.query")
    public CommonResult<CourseComonCO<SingleCourseCO>> courseTimeDetail(
            @RequestParam(value = "semId",required = false) Integer semId,
            @Valid @RequestBody CourseQuery courseQuery){
        return null;
    }

    /**
     * 获取一节课的详细信息
     *@param semId 学期id
     *  @param id 课程详情id
     *
     * */
    @GetMapping("/course/table/one")
    @SaCheckPermission("course.table.query")
    public CommonResult<SingleCourseDetailCO> getCourseDetail(
            @RequestParam(value = "id",required = false) Integer id,
            @RequestParam(value = "semId",required = false) Integer semId){
        return null;
    }

    /**
     * 分页获取课程类型
     *@param  courseQuery 课程查询参数
     *
     * */
    @PostMapping("/course/types")
    @SaCheckPermission("course.type.query")
    public CommonResult<PaginationQueryResultCO<CourseType>> pageCourseType(
            @Valid @RequestBody CourseConditionalQuery courseQuery){
        return null;
    }

    /**
     * 获取所有的课程类型的信息
     *
     * */
    @GetMapping("/courses/types/all")
    @SaCheckPermission("course.type.query")
    public CommonResult<CourseComonCO<CourseType>> allCourseType(){
        return null;
    }
}
