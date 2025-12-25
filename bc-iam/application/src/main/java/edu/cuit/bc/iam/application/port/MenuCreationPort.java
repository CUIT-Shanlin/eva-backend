package edu.cuit.bc.iam.application.port;

import edu.cuit.bc.iam.application.contract.dto.cmd.user.NewMenuCmd;

/**
 * 菜单创建端口（写侧持久化/外部依赖）。
 *
 * <p>保持行为不变：父菜单校验、插入顺序、缓存失效时机与异常文案由端口适配器原样搬运旧实现。</p>
 */
public interface MenuCreationPort {

    /**
     * 新建菜单（沿用旧 gateway 语义）。
     *
     * @param cmd 新建菜单命令
     */
    void createMenu(NewMenuCmd cmd);
}

