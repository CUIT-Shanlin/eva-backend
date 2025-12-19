package edu.cuit.bc.evaluation.domain;

/**
 * 发布评教任务过程中的查询类异常（例如：上限判定失败等）。
 */
public class PostEvaTaskQueryException extends RuntimeException {
    public PostEvaTaskQueryException(String message) {
        super(message);
    }
}

