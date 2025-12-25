package edu.cuit.bc.evaluation.domain;

/**
 * 删除评教模板过程中的查询类异常（例如：模板不存在等）。
 */
public class DeleteEvaTemplateQueryException extends RuntimeException {
    public DeleteEvaTemplateQueryException(String message) {
        super(message);
    }
}

