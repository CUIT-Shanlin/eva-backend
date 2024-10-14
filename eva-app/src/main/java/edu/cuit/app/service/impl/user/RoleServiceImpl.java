package edu.cuit.app.service.impl.user;

import com.alibaba.cola.exception.BizException;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.app.convertor.user.RoleBizConvertor;
import edu.cuit.client.api.user.IRoleService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.user.RoleInfoCO;
import edu.cuit.client.dto.clientobject.user.SimpleRoleInfoCO;
import edu.cuit.client.dto.cmd.user.AssignPermCmd;
import edu.cuit.client.dto.cmd.user.NewRoleCmd;
import edu.cuit.client.dto.cmd.user.UpdateRoleCmd;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.domain.gateway.user.RoleQueryGateway;
import edu.cuit.domain.gateway.user.RoleUpdateGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements IRoleService {

    private final RoleQueryGateway roleQueryGateway;
    private final RoleUpdateGateway roleUpdateGateway;

    private final RoleBizConvertor roleBizConvertor;
    private final PaginationBizConvertor paginationBizConvertor;

    @Override
    public PaginationQueryResultCO<RoleInfoCO> page(PagingQuery<GenericConditionalQuery> pagingQuery) {
        PaginationResultEntity<RoleEntity> page = roleQueryGateway.page(pagingQuery);
        return paginationBizConvertor.toPaginationEntity(page,page.getRecords().stream().map(roleBizConvertor::roleEntityToRoleDO).toList());
    }

    @Override
    public RoleInfoCO one(Integer id) {
        return roleQueryGateway.getById(id).map(roleBizConvertor::roleEntityToRoleDO)
                .orElseThrow(() -> new BizException("该角色不存在"));
    }

    @Override
    public List<SimpleRoleInfoCO> all() {
        return roleQueryGateway.allRole();
    }

    @Override
    public List<Integer> roleMenus(Integer roleId) {
        return roleQueryGateway.getRoleMenuIds(roleId);
    }

    @Override
    public void updateInfo(UpdateRoleCmd updateRoleCmd) {
        roleUpdateGateway.updateRoleInfo(updateRoleCmd);
        if (updateRoleCmd.getStatus() != null) updateStatus(Math.toIntExact(updateRoleCmd.getId()),updateRoleCmd.getStatus());
    }

    @Override
    public void updateStatus(Integer roleId, Integer status) {
        roleUpdateGateway.updateRoleStatus(roleId,status);
    }

    @Override
    public void assignPerm(AssignPermCmd assignPermCmd) {
        roleUpdateGateway.assignPerms(assignPermCmd.getRoleId(),assignPermCmd.getMenuIdList());
    }

    @Override
    public void create(NewRoleCmd newRoleCmd) {
        roleUpdateGateway.createRole(newRoleCmd);
    }

    @Override
    public void delete(Integer roleId) {
        roleUpdateGateway.deleteRole(roleId);
    }

    @Override
    public void multipleDelete(List<Integer> ids) {
        roleUpdateGateway.deleteMultipleRole(ids);
    }
}
