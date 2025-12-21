package edu.cuit.bc.iam.application.port;

import edu.cuit.client.dto.cmd.user.UpdateRoleCmd;

/**
 * 角色信息更新端口（写侧持久化/外部依赖）。
 *
 * <p>保持行为不变：校验、DB 写入、缓存失效与日志顺序由端口适配器原样搬运旧实现。</p>
 */
public interface RoleInfoUpdatePort {

    /**
     * 更新角色信息（沿用旧 gateway 语义）。
     *
     * @param cmd 更新角色命令
     */
    void updateRoleInfo(UpdateRoleCmd cmd);
}

