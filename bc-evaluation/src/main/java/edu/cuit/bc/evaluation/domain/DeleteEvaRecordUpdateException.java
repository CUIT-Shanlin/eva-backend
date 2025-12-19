package edu.cuit.bc.evaluation.domain;

/**
 * 删除评教记录过程中的写侧校验/更新类异常（例如：课程缺少必要信息等）。
 */
public class DeleteEvaRecordUpdateException extends RuntimeException {
    public DeleteEvaRecordUpdateException(String message) {
        super(message);
    }
}

