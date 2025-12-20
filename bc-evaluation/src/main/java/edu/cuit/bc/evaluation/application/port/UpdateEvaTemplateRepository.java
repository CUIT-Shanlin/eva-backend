package edu.cuit.bc.evaluation.application.port;

import edu.cuit.bc.evaluation.application.model.UpdateEvaTemplateCommand;

/**
 * 修改评教模板端口（写侧持久化/外部依赖）。
 */
public interface UpdateEvaTemplateRepository {

    /**
     * 修改评教模板。
     *
     * @param command 修改命令
     */
    void update(UpdateEvaTemplateCommand command);
}

