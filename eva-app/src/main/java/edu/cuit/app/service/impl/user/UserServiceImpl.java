package edu.cuit.app.service.impl.user;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cola.exception.BizException;
import com.alibaba.cola.exception.SysException;
import edu.cuit.app.AvatarManager;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.app.convertor.user.RoleBizConvertor;
import edu.cuit.app.convertor.user.UserBizConvertor;
import edu.cuit.app.factory.user.RouterDetailFactory;
import edu.cuit.client.api.user.IUserService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.eva.CourseScoreCO;
import edu.cuit.client.dto.clientobject.eva.UserSingleCourseScoreCO;
import edu.cuit.client.dto.clientobject.user.*;
import edu.cuit.client.dto.cmd.user.AssignRoleCmd;
import edu.cuit.client.dto.cmd.user.NewUserCmd;
import edu.cuit.client.dto.cmd.user.UpdateUserCmd;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.domain.gateway.user.UserUpdateGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements IUserService {

    private final UserQueryGateway userQueryGateway;
    private final UserUpdateGateway userUpdateGateway;
    private final LdapPersonGateway ldapPersonGateway;
    private final CourseQueryGateway courseQueryGateway;
    private final EvaQueryGateway evaQueryGateway;

    private final AvatarManager avatarManager;

    private final UserBizConvertor userBizConvertor;
    private final RoleBizConvertor roleBizConvertor;
    private final PaginationBizConvertor paginationBizConvertor;

    @Override
    public UserInfoCO getOneUserInfo(Integer id) {
        UserEntity user = userQueryGateway.findById(id)
                .orElseThrow(() -> new BizException("该用户不存在"));
        return getUserInfo(user);
    }

    @Override
    public PaginationQueryResultCO<UserInfoCO> pageUserInfo(PagingQuery<GenericConditionalQuery> query) {
        PaginationResultEntity<UserEntity> userEntityPage = userQueryGateway.page(query);
        List<UserInfoCO> results = userEntityPage.getRecords().stream()
                .map(this::getUserInfo)
                .toList();
        return paginationBizConvertor.toPaginationEntity(userEntityPage,results);
    }

    @Override
    public List<UserSingleCourseScoreCO> getOneUserScore(Integer userId, Integer semId) {
        List<SelfTeachCourseCO> courseInfoList = courseQueryGateway.getSelfCourseInfo(userQueryGateway.findUsernameById(userId)
                .orElseThrow(() -> new SysException("找不到用户名")), semId);

        List<UserSingleCourseScoreCO> resultList = new ArrayList<>();

        for (SelfTeachCourseCO course : courseInfoList) {
            List<CourseScoreCO> evaScore = courseQueryGateway.findEvaScore(course.getId(), semId);
            double score = 0;
            int count = 0;
            for (CourseScoreCO courseScoreCO : evaScore) {
                score += courseScoreCO.getAverScore();
            }
            UserSingleCourseScoreCO courseScoreCO = new UserSingleCourseScoreCO();
            courseScoreCO.setScore(new BigDecimal(score)
                    .divide(new BigDecimal(evaScore.size()),2, RoundingMode.HALF_UP)
                    .doubleValue())
                    .setCourseName(course.getName())
                    .setEvaNum(1); //TODO 设置评教数目
        }
        return List.of();
    }

    @Override
    public List<SimpleResultCO> getAllUserInfo() {
        return userQueryGateway.allUser();
    }

    @Override
    public UserInfoCO getSelfUserInfo() {
        String username = (String) StpUtil.getLoginId();
        return getUserInfo(userQueryGateway.findByUsername(username)
                .orElseThrow(() -> {
                    SysException e = new SysException("用户数据查找失败，请联系管理员");
                    log.error("系统异常",e);
                    return e;
                }));
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
            changePassword(id,password);
        }
        userUpdateGateway.updateInfo(cmd);
    }

    @Override
    public void changePassword(Integer userId, String newPassword) {
        if (StrUtil.isBlank(newPassword)) {
            throw new BizException("新密码不能为空");
        }
        ldapPersonGateway.changePassword(userQueryGateway.findUsernameById(userId)
                .orElseThrow(() -> new BizException("用户不存在")),newPassword);
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

    @Override
    public void syncLdap() {
        List<NewUserCmd> cmdList = ldapPersonGateway.findAll().stream()
                .map(userBizConvertor::toNewUserCmd).toList();
        Set<String> usernameSet = new HashSet<>(userQueryGateway.findAllUsername());
        for (NewUserCmd newUserCmd : cmdList) {
            if (usernameSet.contains(newUserCmd.getUsername())) continue;
            userUpdateGateway.createUser(newUserCmd);
        }
    }

    private UserInfoCO getUserInfo(UserEntity user) {
        List<RouterDetailCO> routerDetailList = RouterDetailFactory.createRouterDetail(user);
        UserDetailCO userDetail = userBizConvertor.toUserDetailCO(user);
        List<RoleInfoCO> roleInfoList = user.getRoles().stream()
                .map(roleBizConvertor::roleEntityToRoleInfoCO)
                .toList();
        return new UserInfoCO()
                .setInfo(userDetail)
                .setRoleList(roleInfoList)
                .setRouterList(routerDetailList)
                .setButtonList(user.getPerms());
    }
}
