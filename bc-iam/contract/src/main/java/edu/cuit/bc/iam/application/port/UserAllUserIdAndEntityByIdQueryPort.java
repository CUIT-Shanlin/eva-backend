package edu.cuit.bc.iam.application.port;

/**
 * 用户导出/批处理场景用的组合查询端口（保持行为不变）。
 *
 * <p>说明：为了让依赖方在编译期只依赖 {@code bc-iam-contract}，同时又不改变既有导出链路的调用形态，
 * 这里提供一个“组合端口”聚合两个最小 Port：</p>
 *
 * <ul>
 *   <li>{@link UserAllUserIdQueryPort}：获取全量用户 ID 列表</li>
 *   <li>{@link UserEntityByIdQueryPort}：按 ID 查询用户实体（过渡期返回 {@code Optional<?>} 以避免循环依赖）</li>
 * </ul>
 *
 * <p>具体缓存/切面触发点仍由端口适配器内部委托旧实现承载，确保行为完全不变。</p>
 */
public interface UserAllUserIdAndEntityByIdQueryPort extends UserAllUserIdQueryPort, UserEntityByIdQueryPort {}

