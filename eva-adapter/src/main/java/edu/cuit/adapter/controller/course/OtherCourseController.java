package edu.cuit.adapter.controller.course;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.dto.cmd.course.CourseToListen;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 新建一门课程
 */
@RestController
@RequiredArgsConstructor
@Validated
public class OtherCourseController {

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
     *  @param courseToListen 内涵课程id，以及听课老师集合
     *
     * */
    @PutMapping("/course/table/one/eva")
    @SaCheckPermission("course.table.assignEva")
    public CommonResult<Void> allocateTeacher(
            @RequestParam(value = "semId",required = false) Integer semId,
            @Valid  @RequestBody CourseToListen courseToListen){
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
}
