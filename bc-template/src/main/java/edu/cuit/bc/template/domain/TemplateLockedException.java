package edu.cuit.bc.template.domain;

/**
 * 模板锁定异常：课程已产生评教数据后，不允许再切换课程模板。
 */
public class TemplateLockedException extends RuntimeException {
    public TemplateLockedException(String message) {
        super(message);
    }
}

