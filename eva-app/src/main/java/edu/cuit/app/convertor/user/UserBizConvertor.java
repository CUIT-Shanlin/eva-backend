package edu.cuit.app.convertor.user;

import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.clientobject.user.UserDetailCO;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.infra.convertor.EntityFactory;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * 用户业务对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserBizConvertor {

    UserDetailCO toUserInfoCO(UserEntity userEntity);

    UnqualifiedUserInfoCO toUnqualifiedUserInfoCO(UserEntity userEntity,Integer num);

}
