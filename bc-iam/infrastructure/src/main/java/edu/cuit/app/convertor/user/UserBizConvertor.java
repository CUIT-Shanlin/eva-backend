package edu.cuit.app.convertor.user;

import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.bc.iam.application.contract.dto.clientobject.user.UserDetailCO;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.NewUserCmd;
import edu.cuit.domain.entity.user.LdapPersonEntity;
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

    UserDetailCO toUserDetailCO(edu.cuit.domain.entity.user.biz.UserEntity userEntity);

    UnqualifiedUserInfoCO toUnqualifiedUserInfoCO(edu.cuit.domain.entity.user.biz.UserEntity userEntity, Integer num);

    /**
     * 过渡期桥接方法：用于让调用方在不编译期引用 {@code UserEntity} 的情况下复用既有映射逻辑。
     * <p>
     * 约束：内部仍使用强转，尽量保持历史空值/异常表现一致。
     * </p>
     */
    default UserDetailCO toUserDetailCOObject(Object userEntity) {
        return toUserDetailCO((edu.cuit.domain.entity.user.biz.UserEntity) userEntity);
    }

    /**
     * 过渡期桥接方法：用于让调用方在不编译期引用 {@code UserEntity} 的情况下复用既有映射逻辑。
     * <p>
     * 约束：内部仍使用强转，尽量保持历史空值/异常表现一致。
     * </p>
     */
    default UnqualifiedUserInfoCO toUnqualifiedUserInfoCOObject(Object userEntity, Integer num) {
        return toUnqualifiedUserInfoCO((edu.cuit.domain.entity.user.biz.UserEntity) userEntity, num);
    }

    @Mappings({
            @Mapping(target = "password",ignore = true),
            @Mapping(target = "department",source = "school"),
            @Mapping(target = "profTitle",source = "title"),
            @Mapping(target = "status",constant = "1")
    })
    NewUserCmd toNewUserCmd(LdapPersonEntity ldapPerson);
}
