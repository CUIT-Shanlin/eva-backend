package edu.cuit.adapter.controller;

import edu.cuit.client.api.IDepartmentService;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学院相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class DepartmentController {

    private final IDepartmentService departmentService;

    /**
     * 获取所有学院名称
     */
    @GetMapping("/departments")
    public CommonResult<List<String>> all() {
        return CommonResult.success(departmentService.all());
    }

}
