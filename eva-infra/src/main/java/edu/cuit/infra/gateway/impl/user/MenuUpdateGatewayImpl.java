package edu.cuit.infra.gateway.impl.user;

import com.alibaba.cola.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.client.dto.cmd.user.NewMenuCmd;
import edu.cuit.client.dto.cmd.user.UpdateMenuCmd;
import edu.cuit.domain.gateway.user.MenuUpdateGateway;
import edu.cuit.infra.convertor.user.MenuConvertor;
import edu.cuit.infra.dal.database.dataobject.user.SysMenuDO;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleMenuDO;
import edu.cuit.infra.dal.database.mapper.user.SysMenuMapper;
import edu.cuit.infra.dal.database.mapper.user.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class MenuUpdateGatewayImpl implements MenuUpdateGateway {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    private final MenuConvertor menuConvertor;

    @Override
    public void updateMenuInfo(UpdateMenuCmd cmd) {
        checkMenuId(cmd.getId());
        SysMenuDO menuDO = menuConvertor.toMenuDO(cmd);
        menuMapper.updateById(menuDO);
    }

    @Override
    public void deleteMenu(Integer menuId) {
        checkMenuId(menuId);
        roleMenuMapper.delete(Wrappers.lambdaQuery(SysRoleMenuDO.class).eq(SysRoleMenuDO::getMenuId,menuId));
        menuMapper.deleteById(menuId);
    }

    @Override
    public void deleteMultipleMenu(List<Integer> menuIds) {
        for (Integer menuId : menuIds) {
            checkMenuId(menuId);
        }
        for (Integer menuId : menuIds) {
            roleMenuMapper.delete(Wrappers.lambdaQuery(SysRoleMenuDO.class).eq(SysRoleMenuDO::getMenuId,menuId));
            menuMapper.deleteById(menuId);
        }
    }

    @Override
    public void createMenu(NewMenuCmd cmd) {
        SysMenuDO menuDO = menuConvertor.toMenuDO(cmd);
        menuMapper.insert(menuDO);
    }

    private void checkMenuId(Integer id) {
        LambdaQueryWrapper<SysMenuDO> menuQuery = Wrappers.lambdaQuery();
        menuQuery.select(SysMenuDO::getId).eq(SysMenuDO::getId,id);
        SysMenuDO sysMenuDO = menuMapper.selectOne(menuQuery);
        if (sysMenuDO == null) {
            throw new BizException("菜单id: " + id + " 不存在");
        }
    }
}
