package edu.cuit.bc.evaluation.application.port;

import java.util.List;

/**
 * 删除评教记录端口（写侧持久化/外部依赖）。
 */
public interface DeleteEvaRecordRepository {

    /**
     * 删除评教记录（支持批量）。
     *
     * @param ids 评教记录 ID 列表
     */
    void delete(List<Integer> ids);
}

