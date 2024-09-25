package edu.cuit.adapter.controller.course.query;

import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.course.ModifySingleCourseDetailCO;
import edu.cuit.client.dto.data.course.CourseTime;
import edu.cuit.client.dto.query.condition.MobileCourseQuery;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户课程信息查询相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class QueryUserCourseController {

    /**
     * 获取单个用户教学的课程基础信息
     *  @param semId 学期id
     *  @param id 用户编号id
     * */
    @GetMapping("/courses")
    public CommonResult<List<SimpleResultCO>> getUserCourseInfo(
            @RequestParam(value = "id",required = true) Integer id,
            @RequestParam(value = "semId",required = false) Integer semId){
        return null;
    }

    /**
     * 获取单个用户的教学课程的详细信息
     *  @param semId 学期id
     *  @param id 用户编号id
     * */
    @GetMapping("/courses/detail")
    public CommonResult<List<CourseDetailCO>> getUserCourseDetail(
            @RequestParam(value = "id",required = true) Integer id,
            @RequestParam(value = "semId",required = false) Integer semId){
        return null;
    }

    /**
     * 获取自己的推荐选课
     * @param semId 学期id
     */
    @GetMapping("/courses/suggestion")
    public CommonResult<List<ModifySingleCourseDetailCO>> getSelfCourse(
            @RequestParam(value = "semId",required = false) Integer semId){
        return null;
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
        return null;
    }



}
