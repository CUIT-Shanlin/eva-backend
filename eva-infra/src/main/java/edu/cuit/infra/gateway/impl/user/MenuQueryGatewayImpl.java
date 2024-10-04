package edu.cuit.infra.gateway.impl.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.client.dto.query.condition.MenuConditionalQuery;
import edu.cuit.domain.entity.user.biz.MenuEntity;
import edu.cuit.domain.gateway.user.MenuQueryGateway;
import edu.cuit.infra.convertor.user.MenuConvertor;
import edu.cuit.infra.dal.database.dataobject.user.SysMenuDO;
import edu.cuit.infra.dal.database.mapper.user.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MenuQueryGatewayImpl implements MenuQueryGateway {

    private final SysMenuMapper menuMapper;

    private final MenuConvertor menuConvertor;

    @Override
    public List<MenuEntity> getMenus(MenuConditionalQuery query) {
        LambdaQueryWrapper<SysMenuDO> menuQuery = Wrappers.lambdaQuery();
        menuQuery.like(SysMenuDO::getName,query.getKeyword())
                .or().eq(SysMenuDO::getStatus,query.getStatus());
        return menuMapper.selectList(menuQuery).stream()
                .map(menuConvertor::toMenuEntity)
                .peek(menuEntity -> menuEntity.setChildren(new ArrayList<>(getChildrenMenus(menuEntity.getId()))))
                .toList();
    }

    @Override
    public Optional<MenuEntity> getOne(Integer id) {
        Optional<MenuEntity> menuEntity = Optional.ofNullable(menuConvertor.toMenuEntity(menuMapper.selectById(id)));
        menuEntity.ifPresent(menu -> menu.setChildren(new ArrayList<>(getChildrenMenus(menu.getId()))));
        return menuEntity;
    }

    @Override
    public List<MenuEntity> getChildrenMenus(Integer parentMenuId) {
        LambdaQueryWrapper<SysMenuDO> menuQuery = Wrappers.lambdaQuery();
        // 查询直接子菜单
        menuQuery.eq(SysMenuDO::getParentId,parentMenuId)
                .ne(SysMenuDO::getId,parentMenuId);
        List<MenuEntity> menuEntities = menuMapper.selectList(menuQuery)
                .stream().map(menuConvertor::toMenuEntity).toList();
        //递归填充子菜单
        for (MenuEntity menuEntity : menuEntities) {
            menuEntity.setChildren(new ArrayList<>(getChildrenMenus(menuEntity.getId())));
        }
        return menuEntities;
    }
}
