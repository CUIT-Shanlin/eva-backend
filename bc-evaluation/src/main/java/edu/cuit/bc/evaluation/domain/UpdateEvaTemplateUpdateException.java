package edu.cuit.bc.evaluation.domain;

/**
 * 修改评教模板过程中的写侧校验/更新类异常（例如：指标重复等）。
 */
public class UpdateEvaTemplateUpdateException extends RuntimeException {
    public UpdateEvaTemplateUpdateException(String message) {
        super(message);
    }
}

