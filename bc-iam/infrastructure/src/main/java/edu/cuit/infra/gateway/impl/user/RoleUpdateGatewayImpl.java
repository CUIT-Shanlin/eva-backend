package edu.cuit.infra.gateway.impl.user;

import edu.cuit.bc.iam.application.usecase.AssignRolePermsUseCase;
import edu.cuit.bc.iam.application.usecase.CreateRoleUseCase;
import edu.cuit.bc.iam.application.usecase.DeleteRoleUseCase;
import edu.cuit.bc.iam.application.usecase.DeleteMultipleRoleUseCase;
import edu.cuit.bc.iam.application.usecase.UpdateRoleInfoUseCase;
import edu.cuit.bc.iam.application.usecase.UpdateRoleStatusUseCase;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.NewRoleCmd;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.UpdateRoleCmd;
import edu.cuit.domain.gateway.user.RoleUpdateGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class RoleUpdateGatewayImpl implements RoleUpdateGateway {

    private final AssignRolePermsUseCase assignRolePermsUseCase;
    private final DeleteMultipleRoleUseCase deleteMultipleRoleUseCase;
    private final UpdateRoleInfoUseCase updateRoleInfoUseCase;
    private final UpdateRoleStatusUseCase updateRoleStatusUseCase;
    private final DeleteRoleUseCase deleteRoleUseCase;
    private final CreateRoleUseCase createRoleUseCase;

    @Override
    public void updateRoleInfo(UpdateRoleCmd cmd) {
        updateRoleInfoUseCase.execute(cmd);
    }

    @Override
    public void updateRoleStatus(Integer roleId, Integer status) {
        updateRoleStatusUseCase.execute(roleId, status);
    }

    @Override
    public void deleteRole(Integer roleId) {
        deleteRoleUseCase.execute(roleId);
    }

    @Override
    public void deleteMultipleRole(List<Integer> ids) {
        deleteMultipleRoleUseCase.execute(ids);
    }

    @Override
    public void assignPerms(Integer roleId, List<Integer> menuIds) {
        assignRolePermsUseCase.execute(roleId, menuIds);
    }

    @Override
    public void createRole(NewRoleCmd cmd) {
        createRoleUseCase.execute(cmd);
    }
}
