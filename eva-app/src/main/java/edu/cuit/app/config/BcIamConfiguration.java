package edu.cuit.app.config;

import edu.cuit.bc.iam.application.port.UserCreationPort;
import edu.cuit.bc.iam.application.port.UserBasicQueryPort;
import edu.cuit.bc.iam.application.port.UserDirectoryQueryPort;
import edu.cuit.bc.iam.application.port.UserEntityQueryPort;
import edu.cuit.bc.iam.application.port.UserDeletionPort;
import edu.cuit.bc.iam.application.port.UserInfoUpdatePort;
import edu.cuit.bc.iam.application.port.RoleBatchDeletionPort;
import edu.cuit.bc.iam.application.port.RolePermissionAssignmentPort;
import edu.cuit.bc.iam.application.port.UserRoleAssignmentPort;
import edu.cuit.bc.iam.application.port.UserMenuCacheInvalidationPort;
import edu.cuit.bc.iam.application.port.UserStatusUpdatePort;
import edu.cuit.bc.iam.application.usecase.AllUserUseCase;
import edu.cuit.bc.iam.application.usecase.AssignRoleUseCase;
import edu.cuit.bc.iam.application.usecase.AssignRolePermsUseCase;
import edu.cuit.bc.iam.application.usecase.CreateUserUseCase;
import edu.cuit.bc.iam.application.usecase.DeleteUserUseCase;
import edu.cuit.bc.iam.application.usecase.DeleteMultipleRoleUseCase;
import edu.cuit.bc.iam.application.usecase.FindAllUserIdUseCase;
import edu.cuit.bc.iam.application.usecase.FindAllUsernameUseCase;
import edu.cuit.bc.iam.application.usecase.FindUserIdByUsernameUseCase;
import edu.cuit.bc.iam.application.usecase.GetUserRoleIdsUseCase;
import edu.cuit.bc.iam.application.usecase.FindUsernameByIdUseCase;
import edu.cuit.bc.iam.application.usecase.FindUserByIdUseCase;
import edu.cuit.bc.iam.application.usecase.FindUserByUsernameUseCase;
import edu.cuit.bc.iam.application.usecase.GetUserStatusUseCase;
import edu.cuit.bc.iam.application.usecase.HandleUserMenuCacheUseCase;
import edu.cuit.bc.iam.application.usecase.IsUsernameExistUseCase;
import edu.cuit.bc.iam.application.usecase.PageUserUseCase;
import edu.cuit.bc.iam.application.usecase.UpdateUserInfoUseCase;
import edu.cuit.bc.iam.application.usecase.UpdateUserStatusUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * bc-iam 的组合根（单体阶段由 eva-app 负责装配）。
 */
@Configuration
public class BcIamConfiguration {

    @Bean
    public AssignRoleUseCase assignRoleUseCase(UserRoleAssignmentPort assignmentPort) {
        return new AssignRoleUseCase(assignmentPort);
    }

    @Bean
    public CreateUserUseCase createUserUseCase(UserCreationPort creationPort) {
        return new CreateUserUseCase(creationPort);
    }

    @Bean
    public UpdateUserInfoUseCase updateUserInfoUseCase(UserInfoUpdatePort updatePort) {
        return new UpdateUserInfoUseCase(updatePort);
    }

    @Bean
    public UpdateUserStatusUseCase updateUserStatusUseCase(UserStatusUpdatePort statusUpdatePort) {
        return new UpdateUserStatusUseCase(statusUpdatePort);
    }

    @Bean
    public DeleteUserUseCase deleteUserUseCase(UserDeletionPort deletionPort) {
        return new DeleteUserUseCase(deletionPort);
    }

    @Bean
    public FindUserByIdUseCase findUserByIdUseCase(UserEntityQueryPort queryPort) {
        return new FindUserByIdUseCase(queryPort);
    }

    @Bean
    public FindUserByUsernameUseCase findUserByUsernameUseCase(UserEntityQueryPort queryPort) {
        return new FindUserByUsernameUseCase(queryPort);
    }

    @Bean
    public PageUserUseCase pageUserUseCase(UserEntityQueryPort queryPort) {
        return new PageUserUseCase(queryPort);
    }

    @Bean
    public FindUserIdByUsernameUseCase findUserIdByUsernameUseCase(UserBasicQueryPort queryPort) {
        return new FindUserIdByUsernameUseCase(queryPort);
    }

    @Bean
    public FindUsernameByIdUseCase findUsernameByIdUseCase(UserBasicQueryPort queryPort) {
        return new FindUsernameByIdUseCase(queryPort);
    }

    @Bean
    public GetUserStatusUseCase getUserStatusUseCase(UserBasicQueryPort queryPort) {
        return new GetUserStatusUseCase(queryPort);
    }

    @Bean
    public IsUsernameExistUseCase isUsernameExistUseCase(UserBasicQueryPort queryPort) {
        return new IsUsernameExistUseCase(queryPort);
    }

    @Bean
    public FindAllUserIdUseCase findAllUserIdUseCase(UserDirectoryQueryPort queryPort) {
        return new FindAllUserIdUseCase(queryPort);
    }

    @Bean
    public FindAllUsernameUseCase findAllUsernameUseCase(UserDirectoryQueryPort queryPort) {
        return new FindAllUsernameUseCase(queryPort);
    }

    @Bean
    public AllUserUseCase allUserUseCase(UserDirectoryQueryPort queryPort) {
        return new AllUserUseCase(queryPort);
    }

    @Bean
    public GetUserRoleIdsUseCase getUserRoleIdsUseCase(UserDirectoryQueryPort queryPort) {
        return new GetUserRoleIdsUseCase(queryPort);
    }

    @Bean
    public AssignRolePermsUseCase assignRolePermsUseCase(RolePermissionAssignmentPort assignmentPort) {
        return new AssignRolePermsUseCase(assignmentPort);
    }

    @Bean
    public DeleteMultipleRoleUseCase deleteMultipleRoleUseCase(RoleBatchDeletionPort deletionPort) {
        return new DeleteMultipleRoleUseCase(deletionPort);
    }

    @Bean
    public HandleUserMenuCacheUseCase handleUserMenuCacheUseCase(UserMenuCacheInvalidationPort invalidationPort) {
        return new HandleUserMenuCacheUseCase(invalidationPort);
    }
}
