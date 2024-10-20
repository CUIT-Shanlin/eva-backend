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
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
public class MenuUpdateGatewayImpl implements MenuUpdateGateway {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    private final MenuConvertor menuConvertor;

    @Override
    public void updateMenuInfo(UpdateMenuCmd cmd) {
        SysMenuDO tmp = checkMenuId(cmd.getId());
        SysMenuDO menuDO = menuConvertor.toMenuDO(cmd);
        menuMapper.updateById(menuDO);

        LogUtils.logContent(tmp.getName() + " 权限的信息");
    }

    @Override
    public void deleteMenu(Integer menuId) {
        SysMenuDO tmp = checkMenuId(menuId);
        roleMenuMapper.delete(Wrappers.lambdaQuery(SysRoleMenuDO.class).eq(SysRoleMenuDO::getMenuId,menuId));
        menuMapper.deleteById(menuId);

        LogUtils.logContent(tmp.getName() + " 权限");
    }

    @Override
    public void deleteMultipleMenu(List<Integer> menuIds) {
        List<SysMenuDO> tmp = new ArrayList<>();
        for (Integer menuId : menuIds) {
            tmp.add(checkMenuId(menuId));
        }
        for (Integer menuId : menuIds) {
            roleMenuMapper.delete(Wrappers.lambdaQuery(SysRoleMenuDO.class).eq(SysRoleMenuDO::getMenuId,menuId));
            menuMapper.deleteById(menuId);
        }

        LogUtils.logContent(tmp + " 权限");
    }

    @Override
    public void createMenu(NewMenuCmd cmd) {
        SysMenuDO menuDO = menuConvertor.toMenuDO(cmd);
        menuMapper.insert(menuDO);
    }

    private SysMenuDO checkMenuId(Integer id) {
        LambdaQueryWrapper<SysMenuDO> menuQuery = Wrappers.lambdaQuery();
        menuQuery.select(SysMenuDO::getId,SysMenuDO::getName).eq(SysMenuDO::getId,id);
        SysMenuDO sysMenuDO = menuMapper.selectOne(menuQuery);
        if (sysMenuDO == null) {
            throw new BizException("菜单id: " + id + " 不存在");
        }
        return sysMenuDO;
    }
}
