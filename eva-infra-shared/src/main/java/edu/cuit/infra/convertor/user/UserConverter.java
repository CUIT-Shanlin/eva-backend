package edu.cuit.infra.convertor.user;

import cn.hutool.extra.spring.SpringUtil;
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

    /**
     * 过渡期桥接方法：用于让调用方在不编译期引用 {@link UserEntity} 的情况下复用既有映射逻辑。
     * <p>
     * 约束：不改变 roles Supplier 的调用时机与次数，仅做类型桥接。
     * </p>
     */
    default Object toUserEntityObject(SysUserDO userDO, Supplier<List<RoleEntity>> roles) {
        return toUserEntity(userDO, roles);
    }

    /**
     * 过渡期桥接方法：从“用户对象（实际为 UserEntity）”中取 userId。
     * <p>
     * 约束：内部仍使用 {@link UserEntity} 强转，以尽量保持历史空值/异常表现一致。
     * </p>
     */
    default Integer userIdOf(Object user) {
        return ((UserEntity) user).getId();
    }

    /**
     * 过渡期桥接方法：从“用户对象（实际为 UserEntity）”中取 username。
     * <p>
     * 约束：内部仍使用 {@link UserEntity} 强转，以尽量保持历史空值/异常表现一致。
     * </p>
     * <p>
     * 说明：为避免被 MapStruct 误判为通用类型转换方法（导致编译期歧义），刻意保留一个无业务意义的形参。
     * </p>
     */
    default String usernameOf(Object user, boolean ignored) {
        return ((UserEntity) user).getUsername();
    }

    /**
     * 过渡期桥接方法：从“用户对象（实际为 UserEntity）”中取 status。
     * <p>
     * 约束：内部仍使用 {@link UserEntity} 强转，以尽量保持历史空值/异常表现一致。
     * </p>
     * <p>
     * 说明：为避免被 MapStruct 误判为通用类型转换方法（导致编译期歧义），刻意保留一个无业务意义的形参。
     * </p>
     */
    default Integer statusOf(Object user, boolean ignored) {
        return ((UserEntity) user).getStatus();
    }

    /**
     * 过渡期桥接方法：从“用户对象（实际为 UserEntity）”中取 roles。
     * <p>
     * 约束：内部仍使用 {@link UserEntity} 强转，以尽量保持历史空值/异常表现一致。
     * </p>
     * <p>
     * 说明：为避免被 MapStruct 误判为通用类型转换方法（导致编译期歧义），刻意保留一个无业务意义的形参。
     * </p>
     */
    default List<RoleEntity> rolesOf(Object user, boolean ignored) {
        return ((UserEntity) user).getRoles();
    }

    /**
     * 过渡期桥接方法：Spring Bean 桥接 + setName 桥接。
     * <p>
     * 约束：内部仍使用 {@link UserEntity} 强转与 {@link SpringUtil#getBean(Class)}，以尽量保持历史异常形态与副作用顺序一致。
     * </p>
     */
    default Object springUserEntityWithNameObject(Object name) {
        UserEntity user = SpringUtil.getBean(UserEntity.class);
        user.setName((String) name);
        return user;
    }

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
