package edu.cuit.adapter.controller.eva.query;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.api.eva.IEvaRecordService;
import edu.cuit.client.api.eva.IEvaTaskService;
import edu.cuit.client.api.eva.IEvaTemplateService;
import edu.cuit.client.api.eva.IUserEvaService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.eva.*;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaLogConditionalQuery;
import edu.cuit.client.dto.query.condition.EvaTaskConditionalQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评教信息查询相关接口
 */
@RestController
@RequiredArgsConstructor
@Validated
public class EvaQueryController {
    private final IEvaRecordService iEvaRecordService;
    private final IEvaTemplateService iEvaTemplateService;
    private final IEvaTaskService iEvaTaskService;
    private final IUserEvaService iUserEvaService;

    //评教记录相关
    /**
     *分页获取评教记录+条件查询，keyword模糊查询 教学课程
     * @param semId 学期id
     * @param query 查询dto
     */
    @PostMapping("/evaluate/records")
    @SaCheckPermission("evaluate.record.query")
    public CommonResult<PaginationQueryResultCO<EvaRecordCO>> pageEvaRecord(
        @RequestParam(value = "semId",required = false) Integer semId,
        @RequestBody PagingQuery<EvaLogConditionalQuery> query){
        return CommonResult.success(iEvaRecordService.pageEvaRecord(semId,query));
    }
    //评教任务相关
    /**
     *分页获取评教任务+条件查询 (keyword 模糊查询课程名称)
     * @param semId 学期id
     * @param query 查询dto
     */
    @PostMapping("/evaluate/tasks")
    @SaCheckPermission("evaluate.task.query")
    public CommonResult<PaginationQueryResultCO<EvaTaskBaseInfoCO>> pageEvaUnfinishedTask(
            @RequestParam(value = "semId",required = false) Integer semId,
            @RequestBody PagingQuery<EvaTaskConditionalQuery> query){
        return CommonResult.success(iEvaTaskService.pageEvaUnfinishedTask(semId,query));
    }
    /**
     * 获取自己的所有待办评教任务
     * @param semId 学期id
     * @param keyword 模糊查询课程名称或教学老师姓名
     */
    @GetMapping("/evaluate/tasks/{keyword}")
    public CommonResult<List<EvaTaskDetailInfoCO>> evaSelfTaskInfo(
            @RequestParam(value = "semId",required = false) Integer semId,
            @PathVariable ("keyword") String keyword){
        return CommonResult.success(iEvaTaskService.evaSelfTaskInfo(semId,keyword));
    }
    /**
     * 获取一个评教任务的详细信息
     * @param id 任务id
     */
    @GetMapping("/evaluate/task/{id}")
    public CommonResult<EvaTaskDetailInfoCO> oneEvaTaskInfo(
            @PathVariable ("id") Integer id){
        return CommonResult.success(iEvaTaskService.oneEvaTaskInfo(id));
    }
    //评教模板相关
    /**
     *分页获取评教模板信息
     * @param semId 学期id
     * @param query 查询dto
     */
    @PostMapping("/evaluate/templates")
    @SaCheckPermission("evaluate.template.query")
    public CommonResult<PaginationQueryResultCO<EvaTemplateCO>> pageEvaTemplate(
            @RequestParam(value = "semId",required = false) Integer semId,
            @RequestBody PagingQuery<GenericConditionalQuery> query){
        return CommonResult.success(iEvaTemplateService.pageEvaTemplate(semId,query));
    }
    /**
     * 获取所有模板的基础信息，仅包含名称和id信息
     */
    @GetMapping("/evaluate/template/all")
    public CommonResult<List<SimpleResultCO>> evaAllTemplate (){
        return CommonResult.success(iEvaTemplateService.evaAllTemplate());
    }
    /**
     * 获取一个任务对应的评教模板
     * @param taskId 任务id
     * @param semId 学期id
     */
    @GetMapping("/evaluate/template/{taskId}")
    public CommonResult<String> getTaskTemplate(
            @PathVariable ("taskId") Integer taskId,
            @RequestParam(value = "semId",required = false) Integer semId){
        return CommonResult.success(iEvaTemplateService.evaTemplateByTaskId(taskId,semId));
    }
    //用户评教相关
    /**
     * 获取自己的评教记录
     * @param semId 学期id
     * @param keyword 模糊查询的关键字，模糊查询课程名称或教学老师姓名
     */
    @GetMapping("/evaluate/records/{keyword}")
    public CommonResult<List<EvaRecordCO>> getEvaLogInfo(
            @RequestParam(value = "semId",required = false) Integer semId,
            @PathVariable ("keyword") String keyword){
        return CommonResult.success(iUserEvaService.getEvaLogInfo(semId,keyword));
    }
    /**
     * 获取对于自己进行的评教的记录
     * @param courseId 筛选的该用户教学的课程的id
     * @param semId 学期id
     */
    @GetMapping("/evaluate/records/opposite")
    public CommonResult<List<EvaRecordCO>> getEvaLoggingInfo(
            @RequestParam(value = "courseId",required = false) Integer courseId,
            @RequestParam(value = "semId",required = false) Integer semId){
        return CommonResult.success(iUserEvaService.getEvaLoggingInfo(courseId,semId));
    }
}
