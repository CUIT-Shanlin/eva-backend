package edu.cuit.bc.iam.application.port;

import edu.cuit.bc.iam.application.contract.dto.cmd.user.NewUserCmd;

/**
 * 用户创建端口（写侧持久化/外部依赖）。
 *
 * <p>保持行为不变：校验、插入顺序、LDAP、缓存失效等逻辑由端口适配器原样搬运旧实现。</p>
 */
public interface UserCreationPort {

    /**
     * 创建用户（沿用旧 gateway 语义）。
     *
     * @param cmd 新建用户命令
     */
    void createUser(NewUserCmd cmd);
}

