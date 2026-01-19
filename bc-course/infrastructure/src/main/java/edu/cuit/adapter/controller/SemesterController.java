package edu.cuit.adapter.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.api.ISemesterService;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学期相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/semester")
public class SemesterController {

    private final ISemesterService semesterService;

    /**
     * 获取所有已有的学期
     */
    @GetMapping("/all")
    public CommonResult<List<SemesterCO>> all() {
        List<SemesterCO> allSemesters = semesterService.all();
        return CommonResult.success(allSemesters);
    }

    /**
     * 获取当前学期的信息
     */
    @GetMapping("/now")
    public CommonResult<SemesterCO> now() {
        SemesterCO currentSemester = semesterService.now();
        return CommonResult.success(currentSemester);
    }

    /**
     * 获取一个学期的信息
     * @param id 学期id
     */
    @GetMapping("/{id}")
    public CommonResult<SemesterCO> semesterInfo(@PathVariable Integer id) {
        SemesterCO semesterInfo = semesterService.semesterInfo(id);
        return CommonResult.success(semesterInfo);
    }

}
