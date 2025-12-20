package edu.cuit.bc.evaluation.application.port;

import edu.cuit.bc.evaluation.application.model.AddEvaTemplateCommand;

/**
 * 新增评教模板端口（写侧持久化/外部依赖）。
 */
public interface AddEvaTemplateRepository {

    /**
     * 新增评教模板。
     *
     * @param command 新增命令
     */
    void add(AddEvaTemplateCommand command);
}

