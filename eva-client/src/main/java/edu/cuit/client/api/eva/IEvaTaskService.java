package edu.cuit.client.api.eva;

import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskBaseInfoCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import jakarta.validation.Valid;

/**
 * 评教任务相关业务接口
 */
public interface IEvaTaskService {

    //查询

    /**
     *分页获取未完成的评教任务+条件查询
     * @param semId 学期id
     * @param query 查询dto
     */
    PaginationQueryResultCO<EvaTaskBaseInfoCO> pageEvaUnfinishedTask(Integer semId, PagingQuery<GenericConditionalQuery> query);

    //修改

    /**
     * 发起评教任务
     *@param evaInfoCO 评教信息dto
     */
    Void postEvaTask(EvaInfoCO evaInfoCO);
}
