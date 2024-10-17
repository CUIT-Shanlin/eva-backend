package edu.cuit.adapter.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
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

    /**
     * 获取所有已有的学期
     */
    @GetMapping("/all")
    public CommonResult<List<SemesterCO>> all() {
        return null;
    }

    /**
     * 获取当前学期的信息
     */
    @GetMapping("/now")
    public CommonResult<SemesterCO> now() {
        return null;
    }

    /**
     * 获取一个学期的信息
     * @param id 学期id
     */
    @GetMapping("/semester/{id}")
    public CommonResult<SemesterCO> semesterInfo(@PathVariable Integer id) {
        return null;
    }

}
