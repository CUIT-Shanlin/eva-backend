package edu.cuit.app.convertor.user;

import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.clientobject.user.UserDetailCO;
import edu.cuit.client.dto.cmd.user.NewUserCmd;
import edu.cuit.domain.entity.user.LdapPersonEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.infra.convertor.EntityFactory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;

/**
 * 用户业务对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserBizConvertor {

    UserDetailCO toUserDetailCO(UserEntity userEntity);

    UnqualifiedUserInfoCO toUnqualifiedUserInfoCO(UserEntity userEntity,Integer num);

    @Mappings({
            @Mapping(target = "password",ignore = true),
            @Mapping(target = "department",source = "school"),
            @Mapping(target = "profTitle",source = "title"),
            @Mapping(target = "status",constant = "1")
    })
    NewUserCmd toNewUserCmd(LdapPersonEntity ldapPerson);
}
