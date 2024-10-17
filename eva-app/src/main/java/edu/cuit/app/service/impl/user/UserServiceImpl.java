package edu.cuit.app.service.impl.user;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cola.exception.BizException;
import com.alibaba.cola.exception.SysException;
import edu.cuit.app.AvatarManager;
import edu.cuit.app.convertor.user.UserBizConvertor;
import edu.cuit.client.api.user.IUserService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.eva.UserSingleCourseScoreCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserResultCO;
import edu.cuit.client.dto.clientobject.user.UserInfoCO;
import edu.cuit.client.dto.cmd.user.AssignRoleCmd;
import edu.cuit.client.dto.cmd.user.NewUserCmd;
import edu.cuit.client.dto.cmd.user.UpdateUserCmd;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.client.dto.query.condition.UnqualifiedUserConditionalQuery;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.domain.gateway.user.UserUpdateGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserQueryGateway userQueryGateway;
    private final UserUpdateGateway userUpdateGateway;
    private final LdapPersonGateway ldapPersonGateway;

    private final AvatarManager avatarManager;

    private final UserBizConvertor userBizConvertor;

    @Override
    public UserInfoCO getOneUserInfo(Integer id) {
        return null;
    }

    @Override
    public PaginationQueryResultCO<UserInfoCO> pageUserInfo(PagingQuery<GenericConditionalQuery> query) {
        return null;
    }

    @Override
    public PaginationQueryResultCO<UnqualifiedUserInfoCO> pageUnqualifiedUser(Integer type, Integer target, PagingQuery<UnqualifiedUserConditionalQuery> query) {
        return null;
    }

    @Override
    public UnqualifiedUserResultCO getTargetAmountUnqualifiedUser(Integer type, Integer num, Integer target) {
        return null;
    }

    @Override
    public List<UserSingleCourseScoreCO> getOneUserScore(Integer userId, Integer semId) {
        return List.of();
    }

    @Override
    public List<SimpleResultCO> getAllUserInfo() {
        return userQueryGateway.allUser();
    }

    @Override
    public UserInfoCO getSelfUserInfo() {
        return null;
    }

    @Override
    public byte[] getUserAvatar(Integer id) {
        return avatarManager.getUserAvatarBytes(id);
    }

    @Override
    public Boolean isUsernameExist(String username) {
        return userQueryGateway.isUsernameExist(username);
    }

    @Override
    public void uploadUserAvatar(Integer userId, InputStream inputStream) {
        avatarManager.uploadUserAvatar(userId,inputStream);
    }

    @Override
    public void updateInfo(Boolean isUpdatePwd, UpdateUserCmd cmd) {
        int id = Math.toIntExact(cmd.getId());
        if (isUpdatePwd) {
            String password = cmd.getPassword();
            if (StrUtil.isBlank(password)) {
                throw new BizException("新密码不能为空");
            }
            ldapPersonGateway.changePassword(userQueryGateway.findUsernameById(id)
                    .orElseThrow(() -> new BizException("用户不存在")),password);
        }
        userUpdateGateway.updateInfo(cmd);
    }

    @Override
    public void updateStatus(Integer userId, Integer status) {
        userUpdateGateway.updateStatus(userId,status);
    }

    @Override
    public void delete(Integer userId) {
        userUpdateGateway.deleteUser(userId);
    }

    @Override
    public void assignRole(AssignRoleCmd cmd) {
        userUpdateGateway.assignRole(cmd.getUserId(),cmd.getRoleIdList());
    }

    @Override
    public void create(NewUserCmd cmd) {
        userUpdateGateway.createUser(cmd);
    }
}
