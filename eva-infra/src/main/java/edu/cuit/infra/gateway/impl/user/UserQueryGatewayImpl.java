package edu.cuit.infra.gateway.impl.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.toolkit.MPJWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import edu.cuit.bc.iam.application.usecase.FindUserIdByUsernameUseCase;
import edu.cuit.bc.iam.application.usecase.FindUsernameByIdUseCase;
import edu.cuit.bc.iam.application.usecase.AllUserUseCase;
import edu.cuit.bc.iam.application.usecase.FindAllUserIdUseCase;
import edu.cuit.bc.iam.application.usecase.FindAllUsernameUseCase;
import edu.cuit.bc.iam.application.usecase.FindUserByIdUseCase;
import edu.cuit.bc.iam.application.usecase.FindUserByUsernameUseCase;
import edu.cuit.bc.iam.application.usecase.GetUserRoleIdsUseCase;
import edu.cuit.bc.iam.application.usecase.GetUserStatusUseCase;
import edu.cuit.bc.iam.application.usecase.IsUsernameExistUseCase;
import edu.cuit.bc.iam.application.usecase.PageUserUseCase;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.infra.convertor.PaginationConverter;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserRoleDO;
import edu.cuit.infra.dal.database.mapper.user.*;
import edu.cuit.zhuyimeng.framework.cache.aspect.annotation.local.LocalCached;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserQueryGatewayImpl implements UserQueryGateway {

    private final FindAllUserIdUseCase findAllUserIdUseCase;
    private final FindAllUsernameUseCase findAllUsernameUseCase;
    private final AllUserUseCase allUserUseCase;
    private final GetUserRoleIdsUseCase getUserRoleIdsUseCase;

    private final FindUserByIdUseCase findUserByIdUseCase;
    private final FindUserByUsernameUseCase findUserByUsernameUseCase;
    private final PageUserUseCase pageUserUseCase;

    private final FindUserIdByUsernameUseCase findUserIdByUsernameUseCase;
    private final FindUsernameByIdUseCase findUsernameByIdUseCase;
    private final GetUserStatusUseCase getUserStatusUseCase;
    private final IsUsernameExistUseCase isUsernameExistUseCase;

    @Override
    @LocalCached(area = "#{@userCacheConstants.ONE_USER_ID}",key = "#id")
    public Optional<UserEntity> findById(Integer id) {
        // 历史路径：收敛到 bc-iam 用例，旧 gateway 逐步退化为委托壳（保持行为不变）
        return findUserByIdUseCase.execute(id);
    }

    @Override
    @LocalCached(area = "#{@userCacheConstants.ONE_USER_USERNAME}",key = "#username")
    public Optional<UserEntity> findByUsername(String username) {
        // 历史路径：收敛到 bc-iam 用例，旧 gateway 逐步退化为委托壳（保持行为不变）
        return findUserByUsernameUseCase.execute(username);
    }

    @Override
    public Optional<Integer> findIdByUsername(String username) {
        // 历史路径：收敛到 bc-iam 用例，旧 gateway 逐步退化为委托壳（保持行为不变）
        return findUserIdByUsernameUseCase.execute(username);
    }

    @Override
    public Optional<String> findUsernameById(Integer id) {
        // 历史路径：收敛到 bc-iam 用例，旧 gateway 逐步退化为委托壳（保持行为不变）
        return findUsernameByIdUseCase.execute(id);
    }

    @Override
    @LocalCached(key = "#{@userCacheConstants.ALL_USER_ID}")
    public List<Integer> findAllUserId() {
        // 历史路径：收敛到 bc-iam 用例，旧 gateway 逐步退化为委托壳（保持行为不变）
        return findAllUserIdUseCase.execute();
    }

    @Override
    @LocalCached(key = "#{@userCacheConstants.ALL_USER_USERNAME}")
    public List<String> findAllUsername() {
        // 历史路径：收敛到 bc-iam 用例，旧 gateway 逐步退化为委托壳（保持行为不变）
        return findAllUsernameUseCase.execute();
    }

    @Override
    public PaginationResultEntity<UserEntity> page(PagingQuery<GenericConditionalQuery> query) {
        // 历史路径：收敛到 bc-iam 用例，旧 gateway 逐步退化为委托壳（保持行为不变）
        return pageUserUseCase.execute(query);
    }

    @Override
    @LocalCached(key = "#{@userCacheConstants.ALL_USER}")
    public List<SimpleResultCO> allUser() {
        // 历史路径：收敛到 bc-iam 用例，旧 gateway 逐步退化为委托壳（保持行为不变）
        return allUserUseCase.execute();
    }

    @Override
    @LocalCached(area = "#{@userCacheConstants.USER_ROLE}",key = "#userId")
    public List<Integer> getUserRoleIds(Integer userId) {
        // 历史路径：收敛到 bc-iam 用例，旧 gateway 逐步退化为委托壳（保持行为不变）
        return getUserRoleIdsUseCase.execute(userId);
    }

    @Override
    public Boolean isUsernameExist(String username) {
        // 历史路径：收敛到 bc-iam 用例，旧 gateway 逐步退化为委托壳（保持行为不变）
        return isUsernameExistUseCase.execute(username);
    }

    @Override
    public Optional<Integer> getUserStatus(Integer id) {
        // 历史路径：收敛到 bc-iam 用例，旧 gateway 逐步退化为委托壳（保持行为不变）
        return getUserStatusUseCase.execute(id);
    }

}
