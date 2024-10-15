package edu.cuit.infra.convertor;

import edu.cuit.domain.entity.MsgEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.infra.dal.database.dataobject.MsgTipDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;

/**
 * 消息对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MsgConvertor {

    MsgEntity toMsgEntity(MsgTipDO msg, UserEntity sender,UserEntity recipient);

    @Mappings({
            @Mapping(target = "senderId",expression = "java(msgEntity.getSender().getId())"),
            @Mapping(target = "recipientId",expression = "java(msgEntity.getRecipient().getId())")
    })
    MsgTipDO toMsgDO(MsgEntity msgEntity);

}
