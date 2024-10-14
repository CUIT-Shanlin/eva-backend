package edu.cuit.app.service.impl.user;

import edu.cuit.client.api.user.IUserMessageService;
import edu.cuit.client.dto.clientobject.EvaMsgCO;
import edu.cuit.client.dto.cmd.SendWarningMsgCmd;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMessageServiceImpl implements IUserMessageService {

    @Override
    public void sendWarningMessage(SendWarningMsgCmd cmd) {

    }

    @Override
    public List<EvaMsgCO> getCurrentUserMsg(Integer type) {
        return List.of();
    }
}
