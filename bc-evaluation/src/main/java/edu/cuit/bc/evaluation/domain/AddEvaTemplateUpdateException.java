package edu.cuit.bc.evaluation.domain;

/**
 * 新增评教模板过程中的写侧校验/更新类异常（例如：指标重复等）。
 */
public class AddEvaTemplateUpdateException extends RuntimeException {
    public AddEvaTemplateUpdateException(String message) {
        super(message);
    }
}

