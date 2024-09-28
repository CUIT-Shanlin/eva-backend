package edu.cuit.adapter.controller.eva.update;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.dto.clientobject.eva.EvaInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskFormCO;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
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
    //修改
    /**
     * 修改评教模板，注：只有在该评教模板没有分配在课程中 且 评教记录中也没使用过该模板 才可以进行删除或修改！
     * @param evaTemplateCO 评教模板dto
     */
    @PutMapping("/evaluate/template")
    @SaCheckPermission("evaluate.record.update")
    public CommonResult<Void> updateEvaTemplate(
            @Valid @RequestBody EvaTemplateCO evaTemplateCO){
        return null;
    }

    //其他操做
    /**
     * 提交评教表单，完成评教任务
     * @param evaTaskFormCO 评教表单评价分值dto//返回数据类型原来没有刚建的
     */
    @PutMapping("/evaluate/task/form")
    public CommonResult<Void> putEvaTemplate(
            @Valid @RequestBody EvaTaskFormCO evaTaskFormCO){
        return null;
    }
    /**
     * 发起评教任务
     *@param evaInfoCO 评教信息dto
     */
    @PostMapping("/evaluate/task")
    public CommonResult<Void> postEvaTask(
            @Valid @RequestBody EvaInfoCO evaInfoCO){
        return null;
    }
    /**
     * 新建评教模板
     * @param evaTemplateCO 评教模板dto
     */
    @PostMapping("/evaluate/template")
    public CommonResult<Void> addEvaTemplate(
            @Valid @RequestBody EvaTemplateCO evaTemplateCO){
        return null;
    }
    /**
     * 任意取消一个评教任务
     * @param id 课程id
     */
    @PutMapping("/evaluate/task/cancel/{id}")
    @SaCheckPermission("evaluate.task.cancel")
    public CommonResult<Void> cancelEvaTask(
            @PathVariable ("id") Integer id){
        return null;
    }
    /**
     * 取消一个自己的评教任务，后端需要检测是不是自己的评教任务，
     * @param id 任务id
     */
    @PutMapping("/evaluate/task/cancel/my/{id}")
    public CommonResult<Void> cancelMyEvaTask(
            @PathVariable ("id") Integer id){
        return null;
    }

}
