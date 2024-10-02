package edu.cuit.infra.gateway.impl.user;

import edu.cuit.client.dto.cmd.user.NewUserCmd;
import edu.cuit.client.dto.cmd.user.UpdateUserCmd;
import edu.cuit.domain.gateway.user.UserUpdateGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class UserUpdateGatewayImpl implements UserUpdateGateway {

    @Override
    public void updateInfo(UpdateUserCmd cmd) {

    }

    @Override
    public void updateStatus(Integer userId, Integer status) {

    }

    @Override
    public void deleteUser(Integer userId) {

    }

    @Override
    public void assignRole(Integer userId, List<Integer> roleId) {

    }

    @Override
    public void createUser(NewUserCmd cmd) {

    }
}
