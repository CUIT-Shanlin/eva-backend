package edu.cuit.bc.evaluation.domain;

/**
 * 删除评教记录过程中的查询类异常（例如：记录/任务不存在等）。
 */
public class DeleteEvaRecordQueryException extends RuntimeException {
    public DeleteEvaRecordQueryException(String message) {
        super(message);
    }
}

