package edu.cuit.app.service.impl.user;

import com.alibaba.cola.exception.BizException;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.app.convertor.user.RoleBizConvertor;
import edu.cuit.bc.iam.application.contract.api.user.IRoleService;
import edu.cuit.bc.iam.application.usecase.AssignRolePermsUseCase;
import edu.cuit.bc.iam.application.usecase.CreateRoleUseCase;
import edu.cuit.bc.iam.application.usecase.DeleteMultipleRoleUseCase;
import edu.cuit.bc.iam.application.usecase.DeleteRoleUseCase;
import edu.cuit.bc.iam.application.usecase.UpdateRoleInfoUseCase;
import edu.cuit.bc.iam.application.usecase.UpdateRoleStatusUseCase;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.bc.iam.application.contract.dto.clientobject.user.RoleInfoCO;
import edu.cuit.bc.iam.application.contract.dto.clientobject.user.SimpleRoleInfoCO;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.AssignPermCmd;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.NewRoleCmd;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.UpdateRoleCmd;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.domain.gateway.user.RoleQueryGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements IRoleService {

    private final RoleQueryGateway roleQueryGateway;

    private final UpdateRoleInfoUseCase updateRoleInfoUseCase;
    private final UpdateRoleStatusUseCase updateRoleStatusUseCase;
    private final AssignRolePermsUseCase assignRolePermsUseCase;
    private final CreateRoleUseCase createRoleUseCase;
    private final DeleteRoleUseCase deleteRoleUseCase;
    private final DeleteMultipleRoleUseCase deleteMultipleRoleUseCase;

    private final RoleBizConvertor roleBizConvertor;
    private final PaginationBizConvertor paginationBizConvertor;

    @Override
    @Transactional
    public PaginationQueryResultCO<RoleInfoCO> page(PagingQuery<GenericConditionalQuery> pagingQuery) {
        PaginationResultEntity<RoleEntity> page = roleQueryGateway.page(pagingQuery);
        return paginationBizConvertor.toPaginationEntity(page,page.getRecords().stream().map(roleBizConvertor::roleEntityToRoleInfoCO).toList());
    }

    @Override
    @Transactional
    public RoleInfoCO one(Integer id) {
        return roleQueryGateway.getById(id).map(roleBizConvertor::roleEntityToRoleInfoCO)
                .orElseThrow(() -> new BizException("该角色不存在"));
    }

    @Override
    @Transactional
    public List<SimpleRoleInfoCO> all() {
        return roleQueryGateway.allRole();
    }

    @Override
    @Transactional
    public List<Integer> roleMenus(Integer roleId) {
        return roleQueryGateway.getRoleMenuIds(roleId);
    }

    @Override
    @Transactional
    public void updateInfo(UpdateRoleCmd updateRoleCmd) {
        updateRoleInfoUseCase.execute(updateRoleCmd);
        if (updateRoleCmd.getStatus() != null) updateStatus(Math.toIntExact(updateRoleCmd.getId()),updateRoleCmd.getStatus());
    }

    @Override
    @Transactional
    public void updateStatus(Integer roleId, Integer status) {
        updateRoleStatusUseCase.execute(roleId, status);
    }

    @Override
    @Transactional
    public void assignPerm(AssignPermCmd assignPermCmd) {
        assignRolePermsUseCase.execute(assignPermCmd.getRoleId(), assignPermCmd.getMenuIdList());
    }

    @Override
    @Transactional
    public void create(NewRoleCmd newRoleCmd) {
        createRoleUseCase.execute(newRoleCmd);
    }

    @Override
    @Transactional
    public void delete(Integer roleId) {
        deleteRoleUseCase.execute(roleId);
    }

    @Override
    @Transactional
    public void multipleDelete(List<Integer> ids) {
        deleteMultipleRoleUseCase.execute(ids);
    }
}
