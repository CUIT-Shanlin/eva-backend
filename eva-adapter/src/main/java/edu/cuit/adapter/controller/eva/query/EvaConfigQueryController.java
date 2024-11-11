package edu.cuit.adapter.controller.eva.query;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.api.eva.IEvaConfigService;
import edu.cuit.client.dto.data.EvaConfig;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 动态配置查询相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class EvaConfigQueryController {

    private final IEvaConfigService evaConfigService;

    /**
     * 查询评教相关配置文件
     */
    @GetMapping("/evaluate/config")
    @SaCheckPermission("evaluate.config.query")
    public CommonResult<EvaConfig> getEvaConfig() {
        return CommonResult.success(evaConfigService.getEvaConfig());
    }

}
