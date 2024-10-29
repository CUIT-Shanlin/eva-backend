package edu.cuit.infra.gateway.impl;

import com.alibaba.cola.exception.SysException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.cuit.client.bo.SysLogBO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.log.SysLogEntity;
import edu.cuit.domain.entity.log.SysLogModuleEntity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.LogGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.infra.convertor.LogConverter;
import edu.cuit.infra.convertor.PaginationConverter;
import edu.cuit.infra.convertor.user.RoleConverter;
import edu.cuit.infra.convertor.user.UserConverter;
import edu.cuit.infra.dal.database.dataobject.log.SysLogDO;
import edu.cuit.infra.dal.database.dataobject.log.SysLogModuleDO;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserRoleDO;
import edu.cuit.infra.dal.database.mapper.log.SysLogMapper;
import edu.cuit.infra.dal.database.mapper.log.SysLogModuleMapper;
import edu.cuit.infra.dal.database.mapper.user.SysRoleMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserRoleMapper;
import edu.cuit.infra.util.QueryUtils;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Component
@Slf4j
public class LogGatewayImpl implements LogGateway {
    private final SysLogMapper logMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final RoleConverter roleConverter;
    private final SysUserRoleMapper userRoleMapper;
    private final UserConverter userConverter;
    private final SysLogModuleMapper logModuleMapper;
    private final LogConverter logConverter;
    private final PaginationConverter pageConverter;

    private final UserQueryGateway userQueryGateway;

    private final Executor executor;

    @Override
    public PaginationResultEntity<SysLogEntity> page(PagingQuery<GenericConditionalQuery> query, Integer moduleId) {
        QueryWrapper<SysLogDO> wrapper = new QueryWrapper<>();
        QueryUtils.fileCreateTimeQuery(wrapper, query.getQueryObj());
        if (query.getQueryObj().getKeyword() != null) {
            wrapper.like("content", query.getQueryObj().getKeyword());
        }
        Page<SysLogDO> page = new Page<>(query.getPage(), query.getSize());
        if (moduleId >= 0) {
            wrapper.eq("module_id", moduleId);
        }
        Page<SysLogDO> logPage = logMapper.selectPage(page, wrapper);
        if (logPage.getRecords().isEmpty()) throw new QueryException("没有找到日志记录");
        List<SysLogDO> records = logPage.getRecords();
        List<SysLogEntity> logEntities = new ArrayList<>();
        for (SysLogDO record : records) {
            logEntities.add(toSysLogEntity(record));
        }
        return pageConverter.toPaginationEntity(logPage, logEntities);
    }

    @Override
    public Optional<SysLogModuleEntity> getModuleByName(String name) {
        LambdaQueryWrapper<SysLogModuleDO> moduleQuery = Wrappers.lambdaQuery();
        moduleQuery.eq(SysLogModuleDO::getName, name);
        SysLogModuleDO sysLogModuleDO = logModuleMapper.selectOne(moduleQuery);
        if (sysLogModuleDO == null) {
            SysException e = new SysException(name + " 日志模块不存在，请联系管理员");
            log.error("发生系统异常", e);
            throw e;
        }
        return Optional.of(logConverter.toModuleEntity(sysLogModuleDO));
    }

    @Override
    public List<SysLogModuleEntity> getModules() {
        List<SysLogModuleDO> sysLogModuleDOS = logModuleMapper.selectList(null);
        if (sysLogModuleDOS.isEmpty()) throw new QueryException("日志模块中暂时还没有信息");
        return sysLogModuleDOS.stream().map(logConverter::toModuleEntity).toList();
    }

    @Override
    public void insertLog(SysLogBO logBO) {
        CompletableFuture.runAsync(() -> {
            SysLogDO logDO = logConverter.toLogDO(logBO, userQueryGateway.findIdByUsername(logBO.getUserId()).orElse(null));
            logMapper.insert(logDO);
        }, executor);
    }

    private SysLogEntity toSysLogEntity(SysLogDO logDO) {
        SysLogModuleDO sysLogModuleDO = logModuleMapper.selectById(logDO.getModuleId());
        if (sysLogModuleDO == null) throw new QueryException("日志模块不存在");
        SysLogModuleEntity moduleEntity = logConverter.toModuleEntity(sysLogModuleDO);
        SysUserDO sysUserDO = userMapper.selectById(logDO.getUserId());
        List<Integer> roleIds = userRoleMapper.selectList(new QueryWrapper<SysUserRoleDO>().eq("user_id", logDO.getUserId()))
                .stream().map(SysUserRoleDO::getRoleId).toList();
        List<SysRoleDO> roleList = roleIds.isEmpty() ? List.of() : roleMapper.selectList(new QueryWrapper<SysRoleDO>().in("id", roleIds));
        List<RoleEntity> roleEntities = roleList.stream().map(roleConverter::toRoleEntity).toList();
        UserEntity userEntity = userConverter.toUserEntity(sysUserDO, () -> roleEntities);
        return logConverter.toLogEntity(logDO, moduleEntity, userEntity);

    }
}
