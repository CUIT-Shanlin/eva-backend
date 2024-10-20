package edu.cuit.adapter.controller.eva.update;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.api.eva.IEvaRecordService;
import edu.cuit.client.api.eva.IEvaTaskService;
import edu.cuit.client.api.eva.IEvaTemplateService;
import edu.cuit.client.dto.clientobject.eva.EvaInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskFormCO;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.common.enums.LogModule;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import edu.cuit.zhuyimeng.framework.logging.aspect.annotation.OperateLog;
import edu.cuit.zhuyimeng.framework.logging.aspect.enums.OperateLogType;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 评教相关更新接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class UpdateEvaController {
    private final IEvaRecordService iEvaRecordService;
    private final IEvaTemplateService iEvaTemplateService;
    private final IEvaTaskService iEvaTaskService;
    //修改
    /**
     * 修改评教模板，注：只有在该评教模板没有分配在课程中 且 评教记录中也没使用过该模板 才可以进行删除或修改！
     * @param evaTemplateCO 评教模板dto
     */
    @PutMapping("/evaluate/template")
    @SaCheckPermission("evaluate.template.update")
    @OperateLog(module = LogModule.EVA,type = OperateLogType.UPDATE)
    public CommonResult<Void> updateEvaTemplate(
            @Valid @RequestBody EvaTemplateCO evaTemplateCO){
        iEvaTemplateService.updateEvaTemplate(evaTemplateCO);
        LogUtils.logContent(evaTemplateCO.getName()+" 的评教模板");
        return CommonResult.success(null);
    }

    //其他操做
    /**
     * 提交评教表单，完成评教任务
     * @param evaTaskFormCO 评教表单评价分值dto//返回数据类型原来没有刚建的
     */
    @PutMapping("/evaluate/task/form")
    @OperateLog(module = LogModule.EVA,type = OperateLogType.CREATE)
    public CommonResult<Void> putEvaTemplate(
            @Valid @RequestBody EvaTaskFormCO evaTaskFormCO){
        iEvaRecordService.putEvaTemplate(evaTaskFormCO);
        LogUtils.logContent("ID为"+evaTaskFormCO.getId()+" 的评教记录"+",完成ID"+evaTaskFormCO.getTaskId()+" 的评教任务");
        return CommonResult.success(null);
    }
    /**
     * 发起评教任务
     *@param evaInfoCO 评教信息dto
     */
    @PostMapping("/evaluate/task")
    @OperateLog(module = LogModule.EVA,type = OperateLogType.CREATE)
    public CommonResult<Void> postEvaTask(
            @Valid @RequestBody EvaInfoCO evaInfoCO){
        iEvaTaskService.postEvaTask(evaInfoCO);
        LogUtils.logContent("ID为"+evaInfoCO.getId()+" 的评教任务");
        return CommonResult.success(null);
    }
    /**
     * 新建评教模板
     * @param evaTemplateCO 评教模板dto
     */
    @PostMapping("/evaluate/template")
    @OperateLog(module = LogModule.EVA,type = OperateLogType.CREATE)
    public CommonResult<Void> addEvaTemplate(
            @Valid @RequestBody EvaTemplateCO evaTemplateCO){
        iEvaTemplateService.addEvaTemplate(evaTemplateCO);
        LogUtils.logContent(evaTemplateCO.getName()+" 的评教模板");
        return CommonResult.success(null);
    }
    /**
     * 任意取消一个评教任务
     * @param id 课程id
     */
    @PutMapping("/evaluate/task/cancel/{id}")
    @SaCheckPermission("evaluate.task.cancel")
    @OperateLog(module = LogModule.EVA,type = OperateLogType.DELETE)
    public CommonResult<Void> cancelEvaTask(
            @PathVariable ("id") Integer id){
        iEvaTaskService.cancelEvaTask(id);
        LogUtils.logContent("ID为"+id+" 的评教任务");
        return CommonResult.success(null);
    }
    /**
     * 取消一个自己的评教任务，后端需要检测是不是自己的评教任务，
     * @param id 任务id
     */
    @PutMapping("/evaluate/task/cancel/my/{id}")
    @OperateLog(module = LogModule.EVA,type = OperateLogType.DELETE)
    public CommonResult<Void> cancelMyEvaTask(
            @PathVariable ("id") Integer id){
        iEvaTaskService.cancelMyEvaTask(id);
        LogUtils.logContent("ID为"+id+" 的评教任务");
        return CommonResult.success(null);
    }

}
