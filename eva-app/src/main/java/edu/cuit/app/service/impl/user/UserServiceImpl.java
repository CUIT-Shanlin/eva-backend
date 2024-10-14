package edu.cuit.app.service.impl.user;

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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements IUserService {

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
    public SimpleResultCO getAllUserInfo() {
        return null;
    }

    @Override
    public UserInfoCO getSelfUserInfo() {
        return null;
    }

    @Override
    public String getUserAvatar(Integer id) {
        return "";
    }

    @Override
    public Boolean isUsernameExist(String username) {
        return null;
    }

    @Override
    public void updateInfo(Boolean isUpdatePwd, UpdateUserCmd cmd) {

    }

    @Override
    public void updateStatus(Integer userId, Integer status) {

    }

    @Override
    public void delete(Integer userId) {

    }

    @Override
    public void assignRole(AssignRoleCmd cmd) {

    }

    @Override
    public void create(NewUserCmd cmd) {

    }
}
