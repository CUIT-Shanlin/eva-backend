package edu.cuit.bc.evaluation.domain;

/**
 * 发布评教任务过程中的写侧校验/更新类异常（例如：时间冲突等）。
 */
public class PostEvaTaskUpdateException extends RuntimeException {
    public PostEvaTaskUpdateException(String message) {
        super(message);
    }
}

