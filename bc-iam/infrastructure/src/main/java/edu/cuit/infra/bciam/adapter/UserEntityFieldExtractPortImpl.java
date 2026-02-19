package edu.cuit.infra.bciam.adapter;

import edu.cuit.bc.iam.application.port.UserEntityFieldExtractPort;
import edu.cuit.infra.convertor.user.UserConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * bc-iam：用户实体字段读取/桥接端口适配器（过渡期，保持历史行为不变）。
 *
 * <p>约束：内部直接委托 {@link UserConverter} 的桥接方法，保持异常/空值/调用顺序不变。</p>
 */
@Component
@RequiredArgsConstructor
public class UserEntityFieldExtractPortImpl implements UserEntityFieldExtractPort {

    private final UserConverter userConverter;

    @Override
    public Integer userIdOf(Object user) {
        return userConverter.userIdOf(user);
    }

    @Override
    public Object springUserEntityWithNameObject(Object name) {
        return userConverter.springUserEntityWithNameObject(name);
    }
}
