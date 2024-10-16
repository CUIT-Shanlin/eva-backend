package edu.cuit.app.convertor;

import edu.cuit.client.dto.clientobject.course.SingleCourseCO;
import edu.cuit.client.dto.data.msg.EvaResponseMsg;
import edu.cuit.client.dto.data.msg.GenericRequestMsg;
import edu.cuit.client.dto.data.msg.GenericResponseMsg;
import edu.cuit.domain.entity.MsgEntity;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.infra.convertor.EntityFactory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 消息业务对象转换器
 */
@Mapper(componentModel = "spring",uses = {EntityFactory.class, UserQueryGateway.class})
public abstract class MsgBizConvertor {

    @Autowired
    protected UserQueryGateway userQueryGateway;

    @Mappings({
            @Mapping(target = "recipientId", expression = "java(msg.getRecipient().getId())"),
            @Mapping(target = "senderId", expression = "java(msg.getSender().getId())"),
            @Mapping(target = "senderName", expression = "java(msg.getSender().getName())")
    })
    public abstract GenericResponseMsg toResponseMsg(MsgEntity msg);

    @Mappings({
            @Mapping(target = "recipientId", expression = "java(msg.getRecipient().getId())"),
            @Mapping(target = "senderId", expression = "java(msg.getSender().getId())"),
            @Mapping(target = "senderName", expression = "java(msg.getSender().getName())"),
            @Mapping(target = "courseInfo", source = "singleCourseCO")
    })
    public abstract EvaResponseMsg toEvaResponseMsg(MsgEntity msg, SingleCourseCO singleCourseCO);

    @Mappings({
            @Mapping(target = "recipient", expression = "java(userQueryGateway.findById(msg.getRecipientId()).orElse(null))"),
            @Mapping(target = "sender", expression = "java(userQueryGateway.findById(msg.getSenderId()).orElse(null))")
    })
    public abstract MsgEntity toMsgEntity(GenericRequestMsg msg);
}
