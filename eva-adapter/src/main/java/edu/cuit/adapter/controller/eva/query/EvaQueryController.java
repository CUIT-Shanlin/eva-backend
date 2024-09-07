package edu.cuit.adapter.controller.eva.query;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.eva.*;
import edu.cuit.client.dto.clientobject.user.UserInfoCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
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

    /**
     *分页获取评教记录+条件查询，keyword模糊查询 教学课程
     * @param semId 学期id
     * @param query 查询dto
     */
    @PostMapping("/evaluate/records")
    @SaCheckPermission("evaluate.record.query")
    public CommonResult<PaginationQueryResultCO<EvaRecordCO>> paginEvaRecord(
        @RequestParam(value = "semId",required = false) Integer semId,
        @RequestBody PagingQuery query){
        return null;
    }
    /**
     *分页获取未完成的评教任务+条件查询
     * @param semId 学期id
     * @param query 查询dto
     */
    @PostMapping("/evaluate/tasks_unfinished")
    @SaCheckPermission("evaluate.task.unfinished.query")
    public CommonResult<PaginationQueryResultCO<EvaTaskBaseInfoCO>> paignEvaUnfishTask(
            @RequestParam(value = "semId",required = false) Integer semId,
            @RequestBody PagingQuery query){
        return null;
    }
    /**
     *分页获取评教模板信息
     * @param semId 学期id
     * @param query 查询dto
     */
    @PostMapping("/evaluate/templates")
    @SaCheckPermission("evaluate.template.query")
    public CommonResult<PaginationQueryResultCO<EvaTemplateCO>> paginEvaTempalte(
            @RequestParam(value = "semId",required = false) Integer semId,
            @RequestBody PagingQuery query){
        return null;
    }
    /**
     *获取评教任务完成情况
     * @param semId 学期id
     */
    @GetMapping("/evaluate/task/situation")
    @SaCheckPermission("evaluate.task.situation.query")
    public CommonResult<EvaSituationCO> evaTaskSituation(
            @RequestParam(value = "semId",required = false) Integer semId){
        return null;
    }
    /**
     * 获取评教分数统计基础信息
     * @param score 指定分数
     * @param semId 学期id
     */
    @GetMapping("/evaluate/score/situation")
    @SaCheckPermission("evaluate.score.query")
    public CommonResult<EvaSituationCO> evaScoreInfo(
            @RequestParam(value = "semId",required = false) Integer semId,
            @RequestParam(value = "score") Integer score){
        return null;
    }
    /**
     * 获取单个用户的待办评教任务，主要用于移动端
     * @param id 用户 ID 编号
     */
    @GetMapping("/evaluate/tasks")
    public CommonResult<EvaTaskDetailInfoCO> evaUnfinishTaskInfo(
            @RequestParam(value = "id",required = false) Integer id){
        return null;
    }
    /**
     * 获取单个用户的评教记录
     * @param id 用户 ID 编号
     */
    @GetMapping("/evaluate/records")
    public CommonResult<EvaRecordCO> OneEvaLogInfo(
            @RequestParam(value = "id",required = false) Integer id){
        return null;
    }
    /**
     * 获取对于单个用户进行的评教记录
     * @param userId 用户 ID 编号
     * @param courseId 筛选的该用户教学的课程的id
     */
    @GetMapping("/evaluate/records/opposite")
    public CommonResult<EvaRecordCO> OneEvaingLogInfo(
            @RequestParam(value = "userId",required = false) Integer userId,
            @RequestParam(value = "courseId",required = false) Integer courseId){
        return null;
    }
    /**
     * 获取所有模板的基础信息，仅包含名称和id信息
     */
    @GetMapping("/evaluate/template/all")
    public CommonResult<List<SimpleResultCO>> evaAllTemplate (){
        return null;
    }
}
