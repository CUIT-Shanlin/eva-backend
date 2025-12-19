package edu.cuit.bc.evaluation.application.port;

import java.util.List;

/**
 * 删除评教模板端口（写侧持久化/外部依赖）。
 */
public interface DeleteEvaTemplateRepository {

    /**
     * 删除评教模板（支持批量）。
     *
     * @param ids 模板 ID 列表
     */
    void delete(List<Integer> ids);
}

