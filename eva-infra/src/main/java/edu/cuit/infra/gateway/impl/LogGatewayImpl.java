package edu.cuit.infra.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.log.SysLogEntity;
import edu.cuit.domain.entity.log.SysLogModuleEntity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.LogGateway;
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
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
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
    @Override
    public PaginationResultEntity<SysLogEntity> page(PagingQuery<GenericConditionalQuery> query) {
        QueryWrapper<SysLogDO> wrapper=new QueryWrapper<SysLogDO>();
        QueryUtils.fileTimeQuery(wrapper,query.getQueryObj());
        if(query.getQueryObj().getKeyword()!=null){
            wrapper.like("content",query.getQueryObj().getKeyword());
        }
        Page<SysLogDO> page=new Page<>(query.getPage(),query.getSize());
        Page<SysLogDO> logPage = logMapper.selectPage(page, wrapper);
        List<SysLogDO> records = logPage.getRecords();
        List<SysLogEntity> logEntities = new ArrayList<>();
        for (SysLogDO record : records) {
            logEntities.add(toSysLogEntity(record));
        }
        pageConverter.toPaginationEntity(logPage, logEntities);
        return null;
    }

    @Override
    public List<SysLogModuleEntity> getModules() {
        List<SysLogModuleDO> sysLogModuleDOS = logModuleMapper.selectList(null);
        List<SysLogModuleEntity> list = sysLogModuleDOS.stream().map(logConverter::toModuleEntity).toList();

        return list;
    }
    
    private SysLogEntity toSysLogEntity(SysLogDO logDO){
        SysLogModuleDO sysLogModuleDO = logModuleMapper.selectById(logDO.getModuleId());
        SysLogModuleEntity moduleEntity = logConverter.toModuleEntity(sysLogModuleDO);
        SysUserDO sysUserDO = userMapper.selectById(logDO.getUserId());
        Stream<Integer> roleIds = userRoleMapper.selectList(new QueryWrapper<SysUserRoleDO>().eq("user_id", logDO.getUserId())).stream().map(SysUserRoleDO::getRoleId);
        List<SysRoleDO> roleList = roleMapper.selectList(new QueryWrapper<SysRoleDO>().in("id", roleIds));
        List<RoleEntity> roleEntities = roleList.stream().map(roleDO -> roleConverter.toRoleEntity(roleDO)).toList();
        UserEntity userEntity = userConverter.toUserEntity(sysUserDO,()-> roleEntities);
        return logConverter.toLogEntity(logDO,moduleEntity,userEntity);

    }
}
