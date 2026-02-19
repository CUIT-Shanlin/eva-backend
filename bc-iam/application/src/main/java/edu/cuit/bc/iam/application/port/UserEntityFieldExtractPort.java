package edu.cuit.bc.iam.application.port;

/**
 * 用户实体字段读取/桥接端口（过渡期）。
 *
 * <p>
 * 约束：仅暴露跨 BC 必需的最小方法集合；保持调用侧不必编译期依赖 {@code UserEntity}。
 * </p>
 */
public interface UserEntityFieldExtractPort {

    Integer userIdOf(Object user);

    Object springUserEntityWithNameObject(Object name);
}
