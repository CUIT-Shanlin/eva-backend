package edu.cuit.adapter.controller.course.update;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.dto.cmd.course.AlignTeacherCmd;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import edu.cuit.client.dto.cmd.course.UpdateCoursesCmd;
import edu.cuit.client.dto.cmd.course.UpdateSingleCourseCmd;
import edu.cuit.client.dto.data.Term;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 课程信息修改相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class UpdateCourseController {

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

    /**
     * 新建一门课程
     *  @param semId 学期id
     *
     * */
    @PostMapping("/course")
    @SaCheckPermission("course.tabulation.add")
    public CommonResult<Void> addCourse(
            @RequestParam(value = "semId",required = false) Integer semId){
        return null;
    }

    /**
     * 分配听课/评教老师
     *  @param semId 学期id
     *  @param alignTeacherCmd 内涵课程id，以及听课老师集合
     *
     * */
    @PutMapping("/course/table/one/eva")
    @SaCheckPermission("course.table.assignEva")
    public CommonResult<Void> allocateTeacher(
            @RequestParam(value = "semId",required = false) Integer semId,
            @Valid  @RequestBody AlignTeacherCmd alignTeacherCmd){
        return null;
    }

    /**
     * 新建一个课程类型
     *
     *  @param courseType 课程类型
     *
     * */
    @PostMapping("/course/type")
    @SaCheckPermission("course.type.add")
    public CommonResult<Void> addCourseType(@Valid @RequestBody CourseType courseType){
        return null;
    }

    /**
     * 导入课表文件
     *
     *  @param file 课表文件
     *
     * */
    @PutMapping("/course/import")
    @SaCheckPermission("course.table.import")
    public CommonResult<Void> imporCourse(MultipartFile file){
        return null;
    }

    /**
     * 判断某学期是否已经导入过课表文件
     *
     *  @param type 用于确定是导入实验课表还是理论课表，0：理论课，1：实验课
     *  @param term 课表文件
     *
     * */
    @PostMapping("/course/table/isImported/{type}")
    @SaCheckPermission("course.table.import")
    public CommonResult<Boolean> isImport(
            @PathVariable(value = "type",required = true) Integer type
            ,@Valid @RequestBody Term term){
        return null;
    }

}
