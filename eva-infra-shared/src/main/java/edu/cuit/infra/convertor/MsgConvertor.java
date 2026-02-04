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

    /**
     * 过渡期桥接方法：用于让调用方在不编译期引用 {@link UserEntity} 的情况下复用既有映射逻辑。
     * <p>
     * 约束：不改变 sender/recipient Supplier 的调用时机与次数，仅做类型桥接。
     * </p>
     */
    default MsgEntity toMsgEntityWithUserObject(MsgTipDO msg, Supplier<?> sender, Supplier<?> recipient) {
        return toMsgEntity(msg, () -> (UserEntity) sender.get(), () -> (UserEntity) recipient.get());
    }

    MsgTipDO toMsgDO(GenericRequestMsg msg);

}
