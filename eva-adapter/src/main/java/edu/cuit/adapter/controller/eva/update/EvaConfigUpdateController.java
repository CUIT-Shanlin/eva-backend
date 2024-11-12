package edu.cuit.adapter.controller.eva.update;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.api.eva.IEvaConfigService;
import edu.cuit.client.dto.data.EvaConfig;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 动态配修覅相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class EvaConfigUpdateController {

    private final IEvaConfigService evaConfigService;

    /**
     * 修改评教相关配置文件
     */
    @PutMapping("/evaluate/config")
    @SaCheckPermission("evaluate.config.update")
    public CommonResult<Void> updateConfig(@RequestBody @Valid EvaConfig evaConfig) {
        evaConfigService.updateEvaConfig(evaConfig);
        return CommonResult.success();
    }

}
