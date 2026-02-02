package edu.cuit.bc.iam.application.port;

import java.util.List;

/**
 * 用户ID列表查询端口（用于广播/批量消息等场景）。
 *
 * <p>保持行为不变：缓存命中/回源顺序与 key 规则等由端口适配器原样委托旧实现承载。</p>
 */
public interface UserAllUserIdQueryPort {

    /**
     * 查询全部用户ID（沿用旧 gateway 语义）。
     */
    List<Integer> findAllUserId();
}

