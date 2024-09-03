package edu.cuit.adapter.controller.course.query;

import edu.cuit.client.dto.query.condition.CourseConditionalQuery;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * 课程信息查询相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class QueryCourseController {
    /**
     * 一个用户信息
     *
     * @param semId 学期id
     */
    @PostMapping("/courses")
    public CommonResult<Object> pageCoursesInfo(@RequestParam("semId") Integer semId,
                                                @RequestBody CourseConditionalQuery courseQuery){
        return null;
    }
}
