package edu.cuit.adapter.controller.course.update;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.dto.data.course.CoursePeriod;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Array;
import java.util.List;
/**
 * 课程删除相关接口
 */

@RestController
@RequiredArgsConstructor
@Validated
public class DeleteCourseController {
    /**
     * 连带删除一门课程
     *  @param semId 学期id
     *  @param id 对应课程编号
     * */
    @DeleteMapping("/course")
    @SaCheckPermission("course.tabulation.delete")
    public CommonResult<Void> delete(
            @RequestParam(value = "id",required = true) Integer id,
            @RequestParam(value = "semId",required = false) Integer semId){
        return null;
    }

    /**
     * 批量删除某节课
     *  @param semId 学期id
     *  @param id 课程详情id
     *  @param coursePeriod 课程的一段时间模型
     * */
    @DeleteMapping("/course/table")
    @SaCheckPermission("course.table.delete")
    public CommonResult<Void> deleteCourses(
            @RequestParam(value = "id",required = true) Integer id,
            @RequestParam(value = "semId",required = false) Integer semId,
            @RequestBody CoursePeriod coursePeriod){
        return null;
    }

    /**
     * 删除一个课程类型
     *  @param id 课程详情id
     * */
    @DeleteMapping("/course/type")
    @SaCheckPermission("course.type.delete")
    public CommonResult<Void> deleteCourseType(
            @RequestParam(value = "id",required = true) Integer id){
        return null;
    }

    /**
     * 批量删除课程类型
     *  @param ids 课程类型数组
     * */
    @DeleteMapping("/course/types")
    @SaCheckPermission("course.type.delete")
    public CommonResult<Void> deleteCoursesType(
            @RequestBody List<Integer> ids){

        return null;
    }

    /**
     * 删除自己的一门课程
     *  @param courseId 课程id
     * */
    @DeleteMapping("/course/my/{courseId}")
    public CommonResult<Void> deleteSelfCourse(
           @PathVariable(value = "courseId") Integer courseId){

        return null;
    }

}
