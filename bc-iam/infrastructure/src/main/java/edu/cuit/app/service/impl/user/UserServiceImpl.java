package edu.cuit.app.service.impl.user;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cola.exception.BizException;
import com.alibaba.cola.exception.SysException;
import edu.cuit.app.AvatarManager;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.app.convertor.user.RoleBizConvertor;
import edu.cuit.app.convertor.user.UserBizConvertor;
import edu.cuit.app.factory.user.RouterDetailFactory;
import edu.cuit.client.api.ISemesterService;
import edu.cuit.client.api.course.ICourseDetailService;
import edu.cuit.client.api.course.ICourseService;
import edu.cuit.client.api.course.IUserCourseService;
import edu.cuit.client.api.eva.IEvaTaskService;
import edu.cuit.bc.iam.application.port.UserBasicQueryPort;
import edu.cuit.bc.iam.application.port.UserDirectoryPageQueryPort;
import edu.cuit.bc.iam.application.port.UserEntityByIdQueryPort;
import edu.cuit.bc.iam.application.port.UserEntityByUsernameQueryPort;
import edu.cuit.bc.iam.application.contract.api.user.IUserService;
import edu.cuit.bc.iam.application.usecase.AssignRoleUseCase;
import edu.cuit.bc.iam.application.usecase.CreateUserUseCase;
import edu.cuit.bc.iam.application.usecase.DeleteUserUseCase;
import edu.cuit.bc.iam.application.usecase.UpdateUserInfoUseCase;
import edu.cuit.bc.iam.application.usecase.UpdateUserStatusUseCase;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.eva.CourseScoreCO;
import edu.cuit.client.dto.clientobject.eva.UserSingleCourseScoreCO;
import edu.cuit.bc.iam.application.contract.dto.clientobject.user.*;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.AssignRoleCmd;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.NewUserCmd;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.UpdatePasswordCmd;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.UpdateUserCmd;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.bc.evaluation.application.port.EvaRecordCountQueryPort;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import edu.cuit.infra.convertor.user.UserConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements IUserService {

    private final UserEntityByIdQueryPort userEntityByIdQueryPort;
    private final UserEntityByUsernameQueryPort userEntityByUsernameQueryPort;
    private final UserBasicQueryPort userBasicQueryPort;
    private final UserDirectoryPageQueryPort userDirectoryPageQueryPort;
    private final AssignRoleUseCase assignRoleUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserInfoUseCase updateUserInfoUseCase;
    private final UpdateUserStatusUseCase updateUserStatusUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final LdapPersonGateway ldapPersonGateway;
    private final CourseQueryGateway courseQueryGateway;
    private final EvaRecordCountQueryPort evaRecordCountQueryPort;

    private final ISemesterService semesterService;
    private final ICourseDetailService courseDetailService;
    private final IUserCourseService userCourseService;
    private final IEvaTaskService evaTaskService;

    private final AvatarManager avatarManager;

    private final UserBizConvertor userBizConvertor;
    private final RoleBizConvertor roleBizConvertor;
    private final PaginationBizConvertor paginationBizConvertor;
    private final UserConverter userConverter;

    @Override
    @Transactional
    public UserInfoCO getOneUserInfo(Integer id) {
        Object user = userEntityByIdQueryPort.findById(id)
                .map(u -> userConverter.castUserEntityObject(u, true))
                .orElseThrow(() -> new BizException("该用户不存在"));
        return getUserInfo(user);
    }

    @Override
    @Transactional
    public PaginationQueryResultCO<UserInfoCO> pageUserInfo(PagingQuery<GenericConditionalQuery> query) {
        PaginationResultEntity<?> userEntityPage = userDirectoryPageQueryPort.page(query);
        List<UserInfoCO> results = userEntityPage.getRecords().stream()
                .map(u -> userConverter.castUserEntityObject(u, true))
                .map(this::getUserInfo)
                .toList();
        return paginationBizConvertor.toPaginationEntity(userEntityPage, results);
    }

    @Override
    @Transactional
    @CheckSemId
    public List<UserSingleCourseScoreCO> getOneUserScore(Integer userId, Integer semId) {
        List<SelfTeachCourseCO> courseInfoList = courseQueryGateway.getSelfCourseInfo(userBasicQueryPort.findUsernameById(userId)
                .orElseThrow(() -> new SysException("找不到用户名")), semId);

        List<UserSingleCourseScoreCO> resultList = new ArrayList<>();

        for (SelfTeachCourseCO course : courseInfoList) {
            List<CourseScoreCO> evaScore = courseQueryGateway.findEvaScore(course.getId());
            if (evaScore.isEmpty()) {
                UserSingleCourseScoreCO courseScoreCO = new UserSingleCourseScoreCO();
                courseScoreCO.setScore(0.0)
                        .setEvaNum(0)
                        .setCourseId(course.getId())
                        .setCourseName(course.getName());
                resultList.add(courseScoreCO);
                continue;
            }
            double score = 0;
            for (CourseScoreCO courseScoreCO : evaScore) {
                score += courseScoreCO.getAverScore();
            }
            UserSingleCourseScoreCO courseScoreCO = new UserSingleCourseScoreCO();
            courseScoreCO.setCourseId(course.getId());
            courseScoreCO.setScore(new BigDecimal(score)
                            .divide(new BigDecimal(evaScore.size()), 2, RoundingMode.HALF_UP)
                            .doubleValue())
                    .setCourseName(course.getName())
                    .setEvaNum(evaRecordCountQueryPort.getEvaNumByCourse(course.getId())
                            .orElse(0));
            resultList.add(courseScoreCO);
        }
        return resultList;
    }

    @Override
    @Transactional
    public List<SimpleResultCO> getAllUserInfo() {
        return userDirectoryPageQueryPort.allUser();
    }

    @Override
    @Transactional
    public UserInfoCO getSelfUserInfo() {
        String username = (String) StpUtil.getLoginId();
        return getUserInfo(userEntityByUsernameQueryPort.findByUsername(username)
                .map(u -> userConverter.castUserEntityObject(u, true))
                .orElseThrow(() -> {
                    SysException e = new SysException("用户数据查找失败，请联系管理员");
                    log.error("系统异常", e);
                    return e;
                }));
    }

    @Override
    @Transactional
    public Integer getIdByUsername(String username) {
        return userBasicQueryPort.findIdByUsername(username).orElseThrow(() -> new BizException("用户名未找到"));
    }

    @Override
    @Transactional
    public byte[] getUserAvatar(Integer id) {
        return avatarManager.getUserAvatarBytes(id);
    }

    @Override
    @Transactional
    public Boolean isUsernameExist(String username) {
        return userBasicQueryPort.isUsernameExist(username);
    }

    @Override
    @Transactional
    public void uploadUserAvatar(Integer userId, InputStream inputStream) {
        avatarManager.uploadUserAvatar(userId, inputStream);
    }

    @Override
    @Transactional
    public void updateInfo(Boolean isUpdatePwd, UpdateUserCmd cmd) {
        int id = Math.toIntExact(cmd.getId());
        String username = userBasicQueryPort.findUsernameById(Math.toIntExact(cmd.getId()))
                .orElseThrow(() -> new BizException("用户ID不存在"));

        if (!userBasicQueryPort.getUserStatus(id).orElseThrow(() -> {
            SysException e = new SysException("用户状态查找失败");
            log.error("发生系统异常", e);
            return e;
        }).equals(cmd.getStatus())) {
            StpUtil.logout(username);
        }
        if (isUpdatePwd) {
            String password = cmd.getPassword();
            if (StrUtil.isBlank(password)) throw new BizException("密码不能为空");
            ldapPersonGateway.changePassword(userBasicQueryPort.findUsernameById(id)
                    .orElseThrow(() -> {
                        SysException e = new SysException("找不到用户名，请联系管理员");
                        log.error("发生系统异常", e);
                        return e;
                    }), password);
        }
        if (isUpdatePwd) {
            try {
                updateUserInfoUseCase.execute(cmd);
            } catch (BizException e) {
                throw new BizException(e.getMessage() + " (但是密码已成功修改)");
            }
        } else updateUserInfoUseCase.execute(cmd);
        if (!userBasicQueryPort.findUsernameById(id).orElseThrow(() -> {
            SysException e = new SysException("用户名查找失败");
            log.error("发生系统异常", e);
            return e;
        }).equals(cmd.getUsername())) {
            StpUtil.logout(username);
        }

    }

    @Override
    @Transactional
    public void updateOwnInfo(UpdateUserCmd cmd) {
        Optional<Integer> id = userBasicQueryPort.findIdByUsername((String) StpUtil.getLoginId());
        cmd.setId(Long.valueOf(id.orElseThrow(() -> {
            SysException e = new SysException("用户id查询失败");
            log.error("发生系统异常", e);
            return e;
        })));
        updateInfo(false, cmd);
    }

    @Override
    @Transactional
    public void changePassword(Integer userId, UpdatePasswordCmd cmd) {
        String username = userBasicQueryPort.findUsernameById(userId)
                .orElseThrow(() -> new BizException("用户不存在"));
        if (!ldapPersonGateway.authenticate(username, cmd.getOldPassword())) {
            throw new BizException("旧密码输入错误");
        }
        if (StrUtil.isBlank(cmd.getPassword())) {
            throw new BizException("新密码不能为空");
        }
        if (cmd.getOldPassword().equals(cmd.getPassword())) {
            throw new BizException("新密码和旧密码不能相同");
        }
        ldapPersonGateway.changePassword(username, cmd.getPassword());
    }

    @Override
    @Transactional
    public void updateStatus(Integer userId, Integer status) {
        updateUserStatusUseCase.execute(userId, status);
        if (status == 0) {
            StpUtil.logout(userBasicQueryPort.findUsernameById(Math.toIntExact(userId))
                    .orElseThrow(() -> new BizException("用户ID不存在")));
        }
    }

    @Override
    @Transactional
    public void delete(Integer userId) {
        List<Integer> semIds = semesterService.all().stream().map(SemesterCO::getId).toList();
        for (Integer semId : semIds) {
            List<Integer> userCourses = userCourseService.getUserCourses(semId, userId);
            for (Integer courseId : userCourses) {
                courseDetailService.delete(semId,courseId);
            }
        }
        evaTaskService.deleteAllTaskByTea(userId);
        deleteUserUseCase.execute(userId);
    }

    @Override
    @Transactional
    public void assignRole(AssignRoleCmd cmd) {
        assignRoleUseCase.execute(cmd.getUserId(), cmd.getRoleIdList());
    }

    @Override
    @Transactional
    public void create(NewUserCmd cmd) {
        createUserUseCase.execute(cmd);
    }

    @Override
    @Transactional
    public void syncLdap() {
        List<NewUserCmd> cmdList = ldapPersonGateway.findAll().stream()
                .map(userBizConvertor::toNewUserCmd).toList();
        Set<String> usernameSet = new HashSet<>(userDirectoryPageQueryPort.findAllUsername());
        for (NewUserCmd newUserCmd : cmdList) {
            if (usernameSet.contains(newUserCmd.getUsername())) continue;
            try {
                createUserUseCase.execute(newUserCmd);
            } catch (BizException ignored) {
            }
        }
    }

    private UserInfoCO getUserInfo(Object user) {
        List<RouterDetailCO> routerDetailList = RouterDetailFactory.createRouterDetail(user);
        UserDetailCO userDetail = userBizConvertor.toUserDetailCOObject(user);
        List<RoleInfoCO> roleInfoList = userConverter.rolesOf(user, true).stream()
                .map(roleBizConvertor::roleEntityToRoleInfoCO)
                .toList();
        return new UserInfoCO()
                .setInfo(userDetail)
                .setRoleList(roleInfoList)
                .setRouterList(routerDetailList)
                .setButtonList(userConverter.permsOf(user, true));
    }
}
