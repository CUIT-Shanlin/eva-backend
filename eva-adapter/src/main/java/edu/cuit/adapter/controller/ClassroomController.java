package edu.cuit.adapter.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import edu.cuit.client.api.IClassroomService;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 教室相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class ClassroomController {

    private final IClassroomService classroomService;

    /**
     * 获取所有教室
     */
    @GetMapping("/classrooms/all")
    @SaCheckLogin
    public CommonResult<List<String>> getAll() {
        return CommonResult.success(classroomService.getAll());
    }

}
