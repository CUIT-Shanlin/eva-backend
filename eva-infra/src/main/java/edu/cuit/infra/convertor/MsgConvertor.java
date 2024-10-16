package edu.cuit.infra.convertor;

import edu.cuit.client.dto.data.msg.GenericRequestMsg;
import edu.cuit.domain.entity.MsgEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.infra.dal.database.dataobject.MsgTipDO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * 消息对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MsgConvertor {

    MsgEntity toMsgEntity(MsgTipDO msg, UserEntity sender,UserEntity recipient);

    MsgTipDO toMsgDO(GenericRequestMsg msg);

}
