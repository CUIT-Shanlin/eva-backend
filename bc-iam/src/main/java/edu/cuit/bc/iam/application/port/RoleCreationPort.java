package edu.cuit.bc.iam.application.port;

import edu.cuit.client.dto.cmd.user.NewRoleCmd;

/**
 * 角色创建端口（写侧持久化/外部依赖）。
 *
 * <p>保持行为不变：校验、DB 写入、缓存失效等副作用顺序由端口适配器原样搬运旧实现。</p>
 */
public interface RoleCreationPort {

    /**
     * 创建角色（沿用旧 gateway 语义）。
     *
     * @param cmd 新增角色命令
     */
    void createRole(NewRoleCmd cmd);
}

