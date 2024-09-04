package edu.cuit.adapter.controller.eva.update;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.client.dto.data.course.CourseTime;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
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
    public CommonResult<Void> updateEvaTempalate(
            @Validated @RequestBody EvaTemplateCO evaTemplateCO){
        return null;
    }

    //其他操做
    /**
     * 提交评教表单，完成评教任务
     * @param evaTemplateCO 评教模板dto
     * @param courseTime 课程时间
     */
    @PutMapping("/evaluate/task")
    public CommonResult<Void> putEvaTempalate(
            @Validated @RequestBody EvaTemplateCO evaTemplateCO,
            @Validated @RequestBody CourseTime courseTime){
        return null;
    }
    /**
     * 发起评教任务
     *@param evaInfoCO 评教信息dto
     */
    @PostMapping("/evaluate/task")
    public CommonResult<Void> postEvaTask(
            @Validated @RequestBody EvaInfoCO evaInfoCO){
        return null;
    }
    /**
     * 新建评教模板
     * @param evaTemplateCO 评教模板dto
     */
    @PostMapping("/evaluate/template")
    public CommonResult<Void> addEvaTempalate(
            @Validated @RequestBody EvaTemplateCO evaTemplateCO){
        return null;
    }
}
