package edu.cuit.app.config;

import edu.cuit.bc.iam.application.port.UserCreationPort;
import edu.cuit.bc.iam.application.port.UserRoleAssignmentPort;
import edu.cuit.bc.iam.application.usecase.AssignRoleUseCase;
import edu.cuit.bc.iam.application.usecase.CreateUserUseCase;
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
}
