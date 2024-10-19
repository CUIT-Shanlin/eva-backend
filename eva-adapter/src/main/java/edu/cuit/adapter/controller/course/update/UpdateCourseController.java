package edu.cuit.adapter.controller.course.update;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.app.service.impl.course.ICourseDetailServiceImpl;
import edu.cuit.app.service.impl.course.ICourseServiceImpl;
import edu.cuit.app.service.impl.course.ICourseTypeServiceImpl;
import edu.cuit.app.service.impl.course.IUserCourseServiceImpl;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
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

import java.io.IOException;
import java.util.List;

/**
 * 课程信息修改相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class UpdateCourseController {
    private final ICourseDetailServiceImpl courseDetailService;
    private final ICourseServiceImpl courseService;
    private final ICourseTypeServiceImpl courseTypeService;
    private final IUserCourseServiceImpl userCourseService;

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
        courseDetailService.updateCourse(semId, updateCourseCmd);
        return CommonResult.success(null);
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
        courseDetailService.updateCourses(semId, updateCoursesCmd);
        return CommonResult.success(null);
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
        courseService.updateSingleCourse(semId, updateSingleCourseCmd);
        return CommonResult.success(null);
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
        courseTypeService.updateCourseType(courseType);
        return CommonResult.success(null);
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
        courseDetailService.addCourse(semId);
        return CommonResult.success(null);
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
        courseService.allocateTeacher(semId, alignTeacherCmd);
        return CommonResult.success(null);
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
        courseTypeService.addCourseType(courseType);
        return CommonResult.success(null);
    }

    /**
     * 导入课表文件
     *  @param file 课表文件
     *  @param type 用于确定是导入实验课表还是理论课表，0：理论课，1：实验课
     *  @param semester 学期模型
     * */
    @PutMapping("/course/import/{type}")
    @SaCheckPermission("course.table.import")
    public CommonResult<Void> imporCourse(
            @RequestParam(value = "file",required = true) MultipartFile file,
            @PathVariable Integer type,
            @RequestParam(value = "semester",required = true) String semester ) throws IOException {
        userCourseService.importCourse(file.getInputStream(), type, semester);
        return CommonResult.success(null);
    }

    /**
     * 判断某学期是否已经导入过课表文件
     *
     *  @param type 用于确定是导入实验课表还是理论课表，0：理论课，1：实验课
     *  @param term 课表时间
     *
     * */
    @PostMapping("/course/table/isImported/{type}")
    @SaCheckPermission("course.table.import")
    public CommonResult<Boolean> isImport(
            @PathVariable(value = "type",required = true) Integer type,
            @Valid @RequestBody Term term){
//        return CommonResult.success(courseTypeService.isImport(type, term));
//        return CommonResult.success(courseDetailService.isImport(type, term));
        return null;

    }

    /**
    * 修改自己的一门课程信息及其课程时段
     *@param selfTeachCourseCO 用于确定是导入实验课表还是理论课表，0：理论课，1：实验课
     *  @param timeList 课表文件
    * */
    @PutMapping("/course/my/info/date")
    public CommonResult<Void> updateSelfCourse(
            @Valid @RequestBody SelfTeachCourseCO selfTeachCourseCO,
            @Valid @RequestBody List<SelfTeachCourseTimeCO> timeList){
        userCourseService.updateSelfCourse(selfTeachCourseCO, timeList);
        return CommonResult.success(null);
    }

    /**
     * 批量新建多节课(已有课程)
     *  @param courseId 课程id
     *  @param timeCO 课程对应授课时间
     *
     * */
    @PostMapping("/course/batch/exist/{courseId}")
    public CommonResult<Void> addExistCoursesDetails(
             @PathVariable Integer courseId
            ,@Valid @RequestBody SelfTeachCourseTimeCO timeCO){
        courseService.addExistCoursesDetails(courseId, timeCO);
        return CommonResult.success(null);
    }

    /**
     * 批量新建多节课(新课程)
     *  @param semId 学期ID
     *  @param teacherId 教学老师ID
     *  @param courseInfo 一门课程的可修改信息(一门课程的可修改信息)
     *  @param dateArr 自己教学的一门课程的一个课程时段模型集合
     * */
    @PostMapping("/course/batch/notExist")
    public CommonResult<Void> addNotExistCoursesDetails(
          @RequestParam(value = "semId",required = false) Integer semId,
          @RequestParam(value = "teacherId",required = true) Integer teacherId,
          @Valid @RequestBody UpdateCourseCmd courseInfo,
          @Valid @RequestBody List<SelfTeachCourseTimeCO> dateArr){
        courseService.addNotExistCoursesDetails(semId, teacherId, courseInfo, dateArr);
        return CommonResult.success(null);
    }

}
