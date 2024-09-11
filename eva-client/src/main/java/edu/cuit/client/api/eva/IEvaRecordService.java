package edu.cuit.client.api.eva;

import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaConditionalQuery;

import java.util.List;

/**
 * 评教记录相关业务接口
 */
public interface IEvaRecordService {
    //查询

    /**
     *分页获取评教记录+条件查询，keyword模糊查询 教学课程
     * @param semId 学期id
     * @param query 查询dto
     */
    PaginationQueryResultCO<EvaRecordCO> pageEvaRecord(Integer semId, PagingQuery<EvaConditionalQuery> query);

    //修改

    /**
     *删除一条评教记录，删除之后，相当于用户没有进行过这次评教
     * @param id 评教记录的 ID 编号
     */
    Void deleteOneEvaLogById(Integer id);
    /**
     * 批量删除评教记录，删除之后，相当于用户没有进行过这次评教，id集合放请求体
     * @param ids 记录数组
     */
    Void deleteEvaLogsById(List<Integer> ids);

}
