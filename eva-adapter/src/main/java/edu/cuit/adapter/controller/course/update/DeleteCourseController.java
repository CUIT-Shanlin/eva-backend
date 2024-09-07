package edu.cuit.adapter.controller.course.update;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Array;
import java.util.List;

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
            @RequestParam("id") Integer id,
            @RequestParam("semId") Integer semId){
        return null;
    }

    /**
     * 批量删除某节课
     *  @param semId 学期id
     *  @param id 对应课程编号
     *  @param startWeek 从哪一周开始删除
     *  @param endWeek 从哪一周结束删除
     * */
    @DeleteMapping("/course/table")
    @SaCheckPermission("course.table.delete")
    public CommonResult<Void> deleteCourses(
            @RequestParam(value = "id",required = false) Integer id,
            @RequestParam(value = "semId",required = false) Integer semId,
            @RequestParam(value = "startWeek",required = false) Integer startWeek,
            @RequestParam(value = "endWeek",required = false) Integer endWeek){
        return null;
    }

    /**
     * 删除一个课程类型
     *  @param id 课程详情id
     * */
    @DeleteMapping("/course/type")
    @SaCheckPermission("course.type.delete")
    public CommonResult<Void> deleteCourseType(
            @RequestParam(value = "id",required = false) Integer id){
        return null;
    }

    /**
     * 批量删除课程类型
     *  @param id 课程数组
     * */
    @DeleteMapping("/course/types")
    @SaCheckPermission("course.type.delete")
    public CommonResult<Void> deleteCoursesType(
            @RequestBody List<Integer> id){

        return null;
    }

}
