package edu.cuit.infra.gateway.impl.user;

import edu.cuit.client.dto.cmd.user.NewRoleCmd;
import edu.cuit.client.dto.cmd.user.UpdateRoleCmd;
import edu.cuit.domain.gateway.user.RoleUpdateGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class RoleUpdateGatewayImpl implements RoleUpdateGateway {

    @Override
    public void updateRoleInfo(UpdateRoleCmd cmd) {

    }

    @Override
    public void updateRoleStatus(Integer roleId, Integer status) {

    }

    @Override
    public void deleteRole(Integer roleId) {

    }

    @Override
    public void deleteMultipleRole(List<Integer> ids) {

    }

    @Override
    public void assignPerms(Integer roleId, List<Integer> menuIds) {

    }

    @Override
    public void createRole(NewRoleCmd cmd) {

    }
}
