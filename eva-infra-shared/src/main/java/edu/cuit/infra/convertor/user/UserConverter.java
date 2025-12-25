package edu.cuit.infra.convertor.user;

import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.NewUserCmd;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.UpdateUserCmd;
import edu.cuit.domain.entity.user.biz.MenuEntity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.infra.convertor.EntityFactory;
import edu.cuit.infra.dal.database.dataobject.user.SysMenuDO;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.function.Supplier;

/**
 * 用户对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class,unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserConverter {

    @Mappings({
        @Mapping(source = "roles",target = "roles")
    })
    UserEntity toUserEntity(SysUserDO userDO, Supplier<List<RoleEntity>> roles);

    @Mapping(target = "extValues", ignore = true)
    SimpleResultCO toUserSimpleResult(SysUserDO userDO);

    @Mappings({
            @Mapping(source = "finishedEvaNum", target = "num"),
            @Mapping(target = "extValues", ignore = true)
    })
    UnqualifiedUserInfoCO toUnqualifiedUserInfo(SysUserDO userDO,Integer finishedEvaNum);

    @Mappings({
            @Mapping(target = "sex",ignore = true),
            @Mapping(target = "createTime",ignore = true),
            @Mapping(target = "updateTime",ignore = true),
            @Mapping(target = "isDeleted",ignore = true)
    })
    SysUserDO toUserDO(UpdateUserCmd cmd);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "sex",ignore = true),
            @Mapping(target = "createTime",ignore = true),
            @Mapping(target = "updateTime",ignore = true),
            @Mapping(target = "isDeleted",ignore = true)
    })
    SysUserDO toUserDO(NewUserCmd cmd);
}
