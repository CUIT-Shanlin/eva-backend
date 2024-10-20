package edu.cuit.adapter.controller.course.update;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.app.service.impl.course.ICourseDetailServiceImpl;
import edu.cuit.app.service.impl.course.ICourseServiceImpl;
import edu.cuit.app.service.impl.course.ICourseTypeServiceImpl;
import edu.cuit.app.service.impl.course.IUserCourseServiceImpl;
import edu.cuit.client.dto.data.course.CoursePeriod;
import edu.cuit.common.enums.LogModule;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import edu.cuit.zhuyimeng.framework.logging.aspect.annotation.OperateLog;
import edu.cuit.zhuyimeng.framework.logging.aspect.enums.OperateLogType;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
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
    private final ICourseDetailServiceImpl courseDetailService;
    private final ICourseServiceImpl courseService;
    private final ICourseTypeServiceImpl courseTypeService;
    private final IUserCourseServiceImpl userCourseService;
    /**
     * 连带删除一门课程
     *  @param semId 学期id
     *  @param id 对应课程编号
     * */
    @DeleteMapping("/course")
    @OperateLog(module = LogModule.COURSE,type = OperateLogType.DELETE)
    @SaCheckPermission("course.tabulation.delete")
    public CommonResult<Void> delete(
            @RequestParam(value = "id",required = true) Integer id,
            @RequestParam(value = "semId",required = false) Integer semId){
        courseDetailService.delete(semId,id);
       return CommonResult.success(null);
    }

    /**
     * 批量删除某节课
     *  @param semId 学期id
     *  @param id 课程详情id
     *  @param coursePeriod 课程的一段时间模型
     * */
    @DeleteMapping("/course/table")
    @OperateLog(module = LogModule.COURSE,type = OperateLogType.DELETE)
    @SaCheckPermission("course.table.delete")
    public CommonResult<Void> deleteCourses(
            @RequestParam(value = "id",required = true) Integer id,
            @RequestParam(value = "semId",required = false) Integer semId,
            @RequestBody CoursePeriod coursePeriod){
        courseService.deleteCourses(semId,id,coursePeriod);
        return CommonResult.success(null);
    }

    /**
     * 删除一个课程类型
     *  @param id 课程类型id
     * */
    @DeleteMapping("/course/type")
    @OperateLog(module = LogModule.COURSE,type = OperateLogType.DELETE)
    @SaCheckPermission("course.type.delete")
    public CommonResult<Void> deleteCourseType(@RequestParam(value = "id",required = true) Integer id){
        courseTypeService.deleteCourseType(id);
        return CommonResult.success(null);
    }

    /**
     * 批量删除课程类型
     *  @param ids 课程类型数组
     * */
    @DeleteMapping("/course/types")
    @OperateLog(module = LogModule.COURSE,type = OperateLogType.DELETE)
    @SaCheckPermission("course.type.delete")
    public CommonResult<Void> deleteCoursesType(
            @RequestBody List<Integer> ids){
        courseTypeService.deleteCoursesType(ids);
        return CommonResult.success(null);
    }

    /**
     * 删除自己的一门课程
     *  @param courseId 课程id
     * */
    @DeleteMapping("/course/my/{courseId}")
    @OperateLog(module = LogModule.COURSE,type = OperateLogType.DELETE)
    public CommonResult<Void> deleteSelfCourse(
           @PathVariable(value = "courseId") Integer courseId){
        userCourseService.deleteSelfCourse(courseId);

        return CommonResult.success(null);
    }

}
