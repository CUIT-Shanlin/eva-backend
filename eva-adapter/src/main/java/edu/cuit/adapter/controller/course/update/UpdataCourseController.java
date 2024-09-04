package edu.cuit.adapter.controller.course.update;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import edu.cuit.client.dto.cmd.course.UpdateCoursesCmd;
import edu.cuit.client.dto.cmd.course.UpdateSingleCourseCmd;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课程信息修改相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class UpdataCourseController {

    /**
     * 修改一门课程
     *@param semId 学期id
     *  @param updateCourseCmd 修改课程信息
     *
     * */
    @PutMapping("/course")
    @SaCheckPermission("course.tabulation.update")
    public CommonResult<Void> updateCourse(
            @RequestParam(value = "semId",required = false) Integer semId,
            @Valid @RequestBody UpdateCourseCmd updateCourseCmd){
        return null;
    }

    /**
     * 批量修改课程的模板
     *@param semId 学期id
     *  @param updateCoursesCmd 批量修改课程信息
     *
     * */
    @PutMapping("/courses/template")
    @SaCheckPermission("course.template.update")
    public CommonResult<Void> updateCourses(
            @RequestParam(value = "semId",required = false) Integer semId,
            @Valid @RequestBody UpdateCoursesCmd updateCoursesCmd){
        return null;
    }

    /**
     * 修改一节课
     *@param semId 学期id
     *  @param updateSingleCourseCmd 修改单节课课程信息
     *
     * */
    @PutMapping("/course/one")
    @SaCheckPermission("course.tabulation.update")
    public CommonResult<Void> updateSingleCourse(
            @RequestParam(value = "semId",required = false) Integer semId,
            @Valid @RequestBody UpdateSingleCourseCmd updateSingleCourseCmd){
        return null;
    }

    /**
     * 修改一节课的类型
     *  @param courseType 修改课课程类型
     *
     * */
    @PutMapping("/course/type")
    @SaCheckPermission("course.type.update")
    public CommonResult<Void> updateCourseType(
            @Valid @RequestBody CourseType courseType){
        return null;
    }


}
