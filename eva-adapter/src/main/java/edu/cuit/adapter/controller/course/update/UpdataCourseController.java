package edu.cuit.adapter.controller.course.update;

import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
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
     *  @param id 用户编号id
     *
     * */
    @PutMapping("/course")
    public CommonResult<Object> updateCourse(@RequestParam("semId") Integer semId,
                                             ){

    }
}
