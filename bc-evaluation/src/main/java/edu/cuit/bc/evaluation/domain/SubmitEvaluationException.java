package edu.cuit.bc.evaluation.domain;

/**
 * 提交评教相关的业务异常（不绑定具体框架异常类型，便于未来拆分微服务/替换框架）。
 */
public class SubmitEvaluationException extends RuntimeException {
    public SubmitEvaluationException(String message) {
        super(message);
    }
}

