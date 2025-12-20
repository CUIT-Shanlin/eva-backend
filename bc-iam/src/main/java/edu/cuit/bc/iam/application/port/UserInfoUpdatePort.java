package edu.cuit.bc.iam.application.port;

import edu.cuit.client.dto.cmd.user.UpdateUserCmd;

/**
 * 用户信息更新端口（写侧持久化/外部依赖）。
 *
 * <p>保持行为不变：校验、DB/LDAP 顺序、缓存与日志由端口适配器原样搬运旧实现。</p>
 */
public interface UserInfoUpdatePort {

    /**
     * 更新用户信息（沿用旧 gateway 语义）。
     *
     * @param cmd 更新用户命令
     */
    void updateInfo(UpdateUserCmd cmd);
}

