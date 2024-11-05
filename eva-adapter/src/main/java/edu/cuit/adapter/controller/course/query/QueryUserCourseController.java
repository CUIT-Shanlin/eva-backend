package edu.cuit.adapter.controller.course.query;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.adapter.controller.course.util.CalculateClassTime;
import edu.cuit.app.service.impl.course.ICourseDetailServiceImpl;
import edu.cuit.app.service.impl.course.ICourseServiceImpl;
import edu.cuit.app.service.impl.course.ICourseTypeServiceImpl;
import edu.cuit.app.service.impl.course.IUserCourseServiceImpl;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.SimpleSubjectResultCO;
import edu.cuit.client.dto.clientobject.course.*;
import edu.cuit.client.dto.data.course.CourseTime;
import edu.cuit.client.dto.query.condition.MobileCourseQuery;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 用户课程信息查询相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class QueryUserCourseController {
    private final ICourseDetailServiceImpl courseDetailService;
    private final ICourseServiceImpl courseService;
    private final ICourseTypeServiceImpl courseTypeService;
    private final IUserCourseServiceImpl userCourseService;

    /**
     * 获取自己教学的课程基础信息
     *  @param semId 学期id
     *
     * */
    @GetMapping("/courses")
    public CommonResult<List<SimpleSubjectResultCO>> getUserCourseInfo(
            @RequestParam(value = "semId",required = false) Integer semId){
        return CommonResult.success(userCourseService.getUserCourseInfo(semId));
    }

    /**
     * 获取单个用户的教学课程的详细信息
     *  @param semId 学期id
     *  @param id 用户编号id
     * */
    @GetMapping("/courses/detail")
    @SaCheckPermission("course.tabulation.query")
    public CommonResult<List<CourseDetailCO>> getUserCourseDetail(
            @RequestParam(value = "id",required = true) Integer id,
            @RequestParam(value = "semId",required = false) Integer semId){
            return CommonResult.success(userCourseService.getUserCourseDetail(id,semId));
    }

    /**
     * 获取自己的推荐选课
     * @param semId 学期id
     */
    @GetMapping("/courses/suggestion")
    public CommonResult<List<RecommendCourseCO>> getSelfCourse(
            @RequestParam(value = "semId",required = false) Integer semId){
        return CommonResult.success(userCourseService.getSelfCourse(semId));
    }

    /**
     * 获取一节课的具体上课时间
     *  @param semId 学期id
     *  @param courseTime 课程时间模型
     * */
    @PostMapping("/course/time")
    public CommonResult<LocalDateTime> getCourseTime(
            @RequestParam(value = "semId",required = false)Integer semId,
            @RequestBody(required = true)CourseTime courseTime){
        String date = courseService.getDate(semId, courseTime.getWeek(), courseTime.getDay());
        String dateTime = date + " 00:00";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);
        return CommonResult.success(CalculateClassTime.calculateClassTime(localDateTime, courseTime.getStartTime()));
    }

    /**
     * 获取自己所有教学的课程的详细信息
     * @param semId 学期id
     * */
    @GetMapping("/courses/my/all/detail")
    public CommonResult<List<SelfTeachCourseCO>> selfCourseDetail(
            @RequestParam(value = "semId",required = true)Integer semId){
        return CommonResult.success(userCourseService.selfCourseDetail(semId));
    }

    /**
     * 获取自己教学的一门课程的课程时段
     * @param courseId 课程id
     * */
    @GetMapping("/course/my/date/{courseId}")
    public CommonResult<List<SelfTeachCourseTimeInfoCO>> selfCourseTime(
            @PathVariable(value = "courseId",required = true) Integer courseId){
        return CommonResult.success(userCourseService.selfCourseTime(courseId));
    }



}
