package edu.cuit.bc.evaluation.domain;

/**
 * 评教任务状态。
 *
 * <p>与数据库 eva_task.status 对齐：</p>
 * <ul>
 *   <li>0：待执行</li>
 *   <li>1：已执行（已提交评教）</li>
 *   <li>2：已撤回</li>
 * </ul>
 */
public enum TaskStatus {
    PENDING(0),
    COMPLETED(1),
    CANCELLED(2);

    private final int code;

    TaskStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static TaskStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 0 -> PENDING;
            case 1 -> COMPLETED;
            case 2 -> CANCELLED;
            default -> null;
        };
    }
}

