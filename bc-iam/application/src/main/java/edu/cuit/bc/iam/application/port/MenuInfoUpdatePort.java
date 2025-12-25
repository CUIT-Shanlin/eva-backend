package edu.cuit.bc.iam.application.port;

import edu.cuit.bc.iam.application.contract.dto.cmd.user.UpdateMenuCmd;

/**
 * 菜单信息更新端口（写侧持久化/外部依赖）。
 *
 * <p>保持行为不变：旧父菜单缓存失效、写入顺序、用户菜单缓存失效与日志顺序由端口适配器原样搬运旧实现。</p>
 */
public interface MenuInfoUpdatePort {

    /**
     * 修改菜单信息（沿用旧 gateway 语义）。
     *
     * @param cmd 修改菜单命令
     */
    void updateMenuInfo(UpdateMenuCmd cmd);
}

