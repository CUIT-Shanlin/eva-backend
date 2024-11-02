package edu.cuit.app.convertor;

import edu.cuit.client.bo.MessageBO;
import edu.cuit.client.dto.clientobject.course.SingleCourseCO;
import edu.cuit.client.dto.cmd.SendMessageCmd;
import edu.cuit.client.dto.data.msg.EvaResponseMsg;
import edu.cuit.client.dto.data.msg.GenericRequestMsg;
import edu.cuit.client.dto.data.msg.GenericResponseMsg;
import edu.cuit.domain.entity.MsgEntity;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.infra.convertor.EntityFactory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 消息业务对象转换器
 */
@Mapper(componentModel = "spring",uses = {EntityFactory.class, UserQueryGateway.class},unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class MsgBizConvertor {

    @Autowired
    protected UserQueryGateway userQueryGateway;

    @Mappings({
            @Mapping(target = "recipientId", expression = "java(msg.getRecipient().getId())"),
            @Mapping(target = "senderId", expression = "java(msg.getSender() == null ? -1 : msg.getSender().getId())"),
            @Mapping(target = "senderName", expression = "java(msg.getSender() == null ? \"\" : ( msg.getIsShowName() == 1 ? msg.getSender().getName() : \"匿名用户\" ))")
    })
    public abstract GenericResponseMsg toResponseMsg(MsgEntity msg);

    @Mappings({
            @Mapping(target = "senderName", expression = "java(msg.getIsShowName() == null ? senderName : (msg.getIsShowName() == 1 ? senderName : \"匿名用户\"))")
    })
    public abstract GenericResponseMsg toResponseMsg(GenericRequestMsg msg,String senderName);

    @Mappings({
            @Mapping(target = "courseInfo",source = "singleCourseCO"),
            @Mapping(target = "senderName", expression = "java(msg.getIsShowName() == null ? senderName : (msg.getIsShowName() == 1 ? senderName : \"匿名用户\"))")
    })
    public abstract EvaResponseMsg toEvaResponseMsg(GenericRequestMsg msg,String senderName,SingleCourseCO singleCourseCO);

    @Mappings({
            @Mapping(target = "isDisplayed", constant = "0"),
            @Mapping(target = "isRead", constant = "0")
    })
    public abstract GenericRequestMsg toRequestMsg(MessageBO msg);

    @Mappings({
            @Mapping(target = "recipientId", expression = "java(msg.getRecipient().getId())"),
            @Mapping(target = "senderId", expression = "java(msg.getSender() == null ? -1 : msg.getSender().getId())"),
            @Mapping(target = "senderName", expression = "java(msg.getSender() == null ? \"\" : ( msg.getIsShowName() == 1 ? msg.getSender().getName() : \"匿名用户\" ))"),
            @Mapping(target = "courseInfo", source = "singleCourseCO"),
            @Mapping(target = "id",source = "msg.id")
    })
    public abstract EvaResponseMsg toEvaResponseMsg(MsgEntity msg, SingleCourseCO singleCourseCO);

    public abstract MessageBO toMessageBO(SendMessageCmd cmd,Integer senderId);

    @Mappings({
            @Mapping(target = "recipient", expression = "java(() -> userQueryGateway.findById(msg.getRecipientId()).orElse(null))"),
            @Mapping(target = "sender", expression = "java(() -> userQueryGateway.findById(msg.getSenderId()).orElse(null))")
    })
    public abstract MsgEntity toMsgEntity(GenericRequestMsg msg);

}
