package edu.cuit.infra.bciam.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.bc.iam.application.port.UserBasicQueryPort;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.enums.cache.UserCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * bc-iam：用户基础信息查询端口适配器（保持历史行为不变：原样搬运旧 gateway 查询逻辑）。
 *
 * <p>当前收敛目标：把 {@code UserQueryGatewayImpl.findIdByUsername/findUsernameById/getUserStatus/isUsernameExist}
 * 从旧 gateway 中抽离，后续让旧 gateway 逐步退化为委托壳；API/异常文案/缓存读写语义保持不变。</p>
 */
@Component
@RequiredArgsConstructor
public class UserBasicQueryPortImpl implements UserBasicQueryPort {

    private final SysUserMapper userMapper;
    private final LocalCacheManager cacheManager;
    private final UserCacheConstants userCacheConstants;

    @Override
    public Optional<Integer> findIdByUsername(String username) {
        Optional<UserEntity> cachedUser = cacheManager.getCache(userCacheConstants.ONE_USER_USERNAME, username);

        if (cachedUser != null && cachedUser.isPresent()) {
            return Optional.ofNullable(cachedUser.get().getId());
        }

        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getId).eq(SysUserDO::getUsername, username);
        return Optional.ofNullable(userMapper.selectOne(userQuery)).map(SysUserDO::getId);
    }

    @Override
    public Optional<String> findUsernameById(Integer id) {
        Optional<UserEntity> cachedUser = cacheManager.getCache(userCacheConstants.ONE_USER_ID, String.valueOf(id));

        if (cachedUser != null && cachedUser.isPresent()) {
            return Optional.ofNullable(cachedUser.get().getUsername());
        }

        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getUsername).eq(SysUserDO::getId, id);
        return Optional.ofNullable(userMapper.selectOne(userQuery)).map(SysUserDO::getUsername);
    }

    @Override
    public Optional<Integer> getUserStatus(Integer id) {
        Optional<UserEntity> cachedUser = cacheManager.getCache(userCacheConstants.ONE_USER_ID, String.valueOf(id));
        if (cachedUser != null && cachedUser.isPresent()) {
            return Optional.ofNullable(cachedUser.get().getStatus());
        }

        LambdaQueryWrapper<SysUserDO> userQuery = Wrappers.lambdaQuery();
        userQuery.select(SysUserDO::getStatus).eq(SysUserDO::getId, id);
        return Optional.ofNullable(userMapper.selectOne(userQuery).getStatus());
    }

    @Override
    public Boolean isUsernameExist(String username) {
        Optional<UserEntity> cachedUser = cacheManager.getCache(userCacheConstants.ONE_USER_USERNAME, username);
        if (cachedUser != null && cachedUser.isPresent()) {
            return true;
        }

        return userMapper.exists(
                Wrappers.lambdaQuery(SysUserDO.class).select(SysUserDO::getUsername).eq(SysUserDO::getUsername, username));
    }
}
