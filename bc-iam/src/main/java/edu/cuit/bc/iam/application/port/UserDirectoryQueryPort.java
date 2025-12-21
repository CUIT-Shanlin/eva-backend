package edu.cuit.bc.iam.application.port;

import edu.cuit.client.dto.clientobject.SimpleResultCO;
import java.util.List;

/**
 * 用户目录查询端口（读侧持久化/外部依赖）。
 *
 * <p>保持行为不变：查询字段、映射规则、缓存 key/area 与命中语义等由旧 gateway 保持，
 * 端口适配器仅原样搬运旧查询逻辑。</p>
 */
public interface UserDirectoryQueryPort {

    /**
     * 查询所有用户ID（沿用旧 gateway 语义）。
     */
    List<Integer> findAllUserId();

    /**
     * 查询所有用户名（沿用旧 gateway 语义）。
     */
    List<String> findAllUsername();

    /**
     * 查询所有用户简要信息（沿用旧 gateway 语义）。
     */
    List<SimpleResultCO> allUser();

    /**
     * 查询用户角色ID列表（沿用旧 gateway 语义）。
     */
    List<Integer> getUserRoleIds(Integer userId);
}

