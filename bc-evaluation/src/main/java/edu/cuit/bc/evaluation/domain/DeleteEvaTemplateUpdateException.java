package edu.cuit.bc.evaluation.domain;

/**
 * 删除评教模板过程中的写侧校验/更新类异常（例如：默认模板/已分配模板不可删等）。
 */
public class DeleteEvaTemplateUpdateException extends RuntimeException {
    public DeleteEvaTemplateUpdateException(String message) {
        super(message);
    }
}

