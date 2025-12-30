package edu.cuit.infra.convertor;

import edu.cuit.client.dto.data.msg.GenericRequestMsg;
import edu.cuit.domain.entity.MsgEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.infra.dal.database.dataobject.MsgTipDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;

import java.util.function.Supplier;

/**
 * 消息对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MsgConvertor {

    @Mappings({
            @Mapping(target = "id",source = "msg.id"),
            @Mapping(target = "createTime",source = "msg.createTime"),
    })
    MsgEntity toMsgEntity(MsgTipDO msg, Supplier<UserEntity> sender, Supplier<UserEntity> recipient);

    MsgTipDO toMsgDO(GenericRequestMsg msg);

}

