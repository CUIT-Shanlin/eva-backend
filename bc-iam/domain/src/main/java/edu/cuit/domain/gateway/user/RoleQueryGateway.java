package edu.cuit.domain.gateway.user;

import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.bc.iam.application.contract.dto.clientobject.user.SimpleRoleInfoCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 角色相关数据门户接口
 */
@Component
public interface RoleQueryGateway {

    /**
     * 获取一个角色信息
     * @param roleId 角色id
     */
    Optional<RoleEntity> getById(Integer roleId);

    /**
     * 分页查询角色
     * @param query 查询模型
     */
    PaginationResultEntity<RoleEntity> page(PagingQuery<GenericConditionalQuery> query);

    /**
     * 获取所有角色信息
     */
    List<SimpleRoleInfoCO> allRole();

    /**
     * 获取角色权限菜单id数组
     * @param roleId 角色id
     */
    List<Integer> getRoleMenuIds(Integer roleId);

    /**
     * 获取默认角色id
     */
    Integer getDefaultRoleId();

}
