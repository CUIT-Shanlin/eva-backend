package edu.cuit.adapter.controller.eva.update;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.api.eva.IEvaRecordService;
import edu.cuit.client.api.eva.IEvaTaskService;
import edu.cuit.client.api.eva.IEvaTemplateService;
import edu.cuit.common.enums.LogModule;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import edu.cuit.zhuyimeng.framework.logging.aspect.annotation.OperateLog;
import edu.cuit.zhuyimeng.framework.logging.aspect.enums.OperateLogType;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评教相关更新接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class DeleteEvaController {
    private final IEvaRecordService iEvaRecordService;
    private final IEvaTemplateService iEvaTemplateService;
    private final IEvaTaskService iEvaTaskService;
    //删除
    /**
     *删除一条评教记录，删除之后，相当于用户没有进行过这次评教
     * @param id 评教记录的 ID 编号
     */
    @DeleteMapping("/evaluate/record")
    @SaCheckPermission("evaluate.record.delete")
    @OperateLog(module = LogModule.EVA,type = OperateLogType.DELETE)
    public CommonResult<Void> deleteOneEvaLogById(
            @RequestParam(value = "id") Integer id){
        iEvaRecordService.deleteOneEvaLogById(id);
        return CommonResult.success(null);
    }
    /**
     * 批量删除评教记录，删除之后，相当于用户没有进行过这次评教，id集合放请求体
     * @param ids 记录数组
     */
    @DeleteMapping("/evaluate/records")
    @SaCheckPermission("evaluate.record.delete")
    @OperateLog(module = LogModule.EVA,type = OperateLogType.DELETE)
    public CommonResult<Void> deleteEvaLogsById(
            @RequestBody List<Integer> ids){
        iEvaRecordService.deleteEvaLogsById(ids);
        return CommonResult.success(null);
    }
    /**
     * 删除评教模板
     * @param templateId 模板的 ID 编号
     */
    @DeleteMapping("/evaluate/template")
    @SaCheckPermission("evaluate.template.delete")
    @OperateLog(module = LogModule.EVA,type = OperateLogType.DELETE)
    public CommonResult<Void> deleteEvaTemplateById(
            @RequestParam(value = "templateId") Integer templateId){
        iEvaTemplateService.deleteEvaTemplateById(templateId);
        return CommonResult.success(null);
    }
    /**
     * 批量删除模板，后端要再次检验是否可被删除
     * @param ids 模板数组
     */
    @DeleteMapping("/evaluate/templates")
    @SaCheckPermission("evaluate.template.delete")
    @OperateLog(module = LogModule.EVA,type = OperateLogType.DELETE)
    public CommonResult<Void> deleteEvaTemplatesById(
            @RequestBody List<Integer> ids ){
        iEvaTemplateService.deleteEvaTemplatesById(ids);
        return CommonResult.success(null);
    }

}
