package edu.cuit.infra.bciam.adapter;

import com.alibaba.cola.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.bc.iam.application.port.UserStatusUpdatePort;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.enums.cache.CourseCacheConstants;
import edu.cuit.infra.enums.cache.UserCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * bc-iam：用户状态更新端口适配器（保持历史行为不变：原样搬运旧 gateway 写流程）。
 */
@Component
@RequiredArgsConstructor
public class UserStatusUpdatePortImpl implements UserStatusUpdatePort {

    private final SysUserMapper userMapper;

    private final LocalCacheManager cacheManager;
    private final UserCacheConstants userCacheConstants;
    private final CourseCacheConstants courseCacheConstants;

    @Override
    public void updateStatus(Integer userId, Integer status) {
        SysUserDO tmp = checkIdExistence(userId);
        checkAdmin(userId);
        LambdaUpdateWrapper<SysUserDO> userUpdate = Wrappers.lambdaUpdate();
        userUpdate.set(SysUserDO::getStatus, status).eq(SysUserDO::getId, userId);
        userMapper.update(userUpdate);

        handleUserUpdateCache(userId);

        LogUtils.logContent(tmp.getName() + " 用户(id:" + tmp.getId() + ")的状态为 " + status);
    }

    private void checkAdmin(Integer userId) {
        String username = getUsername(userId);
        if ("admin".equalsIgnoreCase(username)) {
            throw new BizException("初始管理员账户不允许此操作");
        }
    }

    private void handleUserUpdateCache(Integer userId) {
        String username = getUsername(userId);
        handleUserUpdateCache(userId, username);
    }

    private void handleUserUpdateCache(Integer userId, String username) {
        cacheManager.invalidateCache(userCacheConstants.ONE_USER_ID, String.valueOf(userId));
        cacheManager.invalidateCache(userCacheConstants.ONE_USER_USERNAME, username);
        cacheManager.invalidateCache(null, userCacheConstants.ALL_USER);
        cacheManager.invalidateCache(null, userCacheConstants.ALL_USER_USERNAME);
        cacheManager.invalidateCache(userCacheConstants.USER_ROLE, String.valueOf(userId));
        cacheManager.invalidateCache(null, userCacheConstants.ALL_DEPARTMENT);
        cacheManager.invalidateArea(courseCacheConstants.COURSE_LIST_BY_SEM);
    }

    private SysUserDO checkIdExistence(Integer id) {
        LambdaQueryWrapper<SysUserDO> query = Wrappers.lambdaQuery();
        query.select(SysUserDO::getId, SysUserDO::getName).eq(SysUserDO::getId, id);
        SysUserDO sysUserDO = userMapper.selectOne(query);
        if (sysUserDO == null) {
            throw new BizException("用户id不存在");
        }
        return sysUserDO;
    }

    private String getUsername(Integer id) {
        LambdaQueryWrapper<SysUserDO> query = Wrappers.lambdaQuery();
        query.select(SysUserDO::getUsername).eq(SysUserDO::getId, id);
        SysUserDO sysUserDO = userMapper.selectOne(query);
        if (sysUserDO != null) {
            return sysUserDO.getUsername();
        } else return null;
    }
}
