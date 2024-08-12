package edu.cuit.infra.gateway.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.IdUtil;
import com.alibaba.cola.exception.BizException;
import edu.cuit.domain.entity.user.LdapPersonEntity;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import edu.cuit.infra.convertor.user.UserConvertor;
import edu.cuit.infra.dal.ldap.dataobject.LdapGroupDO;
import edu.cuit.infra.dal.ldap.dataobject.LdapPersonDO;
import edu.cuit.infra.dal.ldap.repo.LdapGroupRepo;
import edu.cuit.infra.dal.ldap.repo.LdapPersonRepo;
import edu.cuit.infra.enums.LdapConstant;
import edu.cuit.infra.util.EvaLdapUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LdapPersonGatewayImpl implements LdapPersonGateway {

    private final LdapTemplate ldapTemplate;
    private final LdapPersonRepo ldapPersonRepo;
    private final UserConvertor userConvertor;
    private final LdapGroupRepo ldapGroupRepo;

    @Override
    public boolean authenticate(String username, String password) {
        EqualsFilter equalsFilter = new EqualsFilter("uid",username);
        return ldapTemplate.authenticate(LdapConstant.USER_BASE_DN,equalsFilter.encode(),password);
    }

    @Override
    public Optional<LdapPersonEntity> findByUsername(String username) {
        Optional<LdapPersonDO> personOpt = ldapPersonRepo.findByUsername(username);
        Optional<LdapPersonEntity> personEntityOpt = personOpt.map(userConvertor::ldapPersonDoToLdapPersonEntity);
        if (personEntityOpt.isEmpty()) return Optional.empty();
        LdapPersonEntity personEntity = personEntityOpt.get();
        personEntity.setIsAdmin(getAdminPersons().contains(personEntity.getUsername()));
        return Optional.of(personEntity);
    }

    @Override
    public List<LdapPersonEntity> findAll() {
        return ldapPersonRepo.findAll().stream().map(userConvertor::ldapPersonDoToLdapPersonEntity).toList();
    }

    @Override
    public void saveUser(LdapPersonEntity user) {
        LdapPersonDO personDO = ldapPersonRepo.findByUsername(user.getUsername()).orElseThrow(() -> new BizException("该用户不存在"));
        BeanUtil.copyProperties(user,personDO, CopyOptions.create().ignoreNullValue().setOverride(true));
        ldapPersonRepo.save(personDO);
    }

    @Override
    public void createUser(LdapPersonEntity user, String password) {
        LdapPersonDO personDO = userConvertor.ldapPersonEntityToLdapPersonDO(user);
        personDO.setUserPassword(password);
        personDO.setId(EvaLdapUtils.getUserLdapNameId(personDO.getUsername()));
        personDO.setCommonName(personDO.getSurname() + personDO.getGivenName());
        personDO.setUidNumber(IdUtil.getSnowflakeNextIdStr());
        personDO.setHomeDirectory("/home/" + user.getUsername());
        ldapTemplate.create(personDO);
    }

    /**
     * 获取所有管理员的用户名
     */
    private List<String> getAdminPersons() {
        Optional<LdapGroupDO> adminGroupOpt = ldapGroupRepo.findByCommonName(EvaLdapUtils.evaLdapProperties.getAdminGroupCn());
        if (adminGroupOpt.isEmpty()) return new ArrayList<>();
        LdapGroupDO adminGroup = adminGroupOpt.get();
        return adminGroup.getMembers();
    }

    /**
     * 获取管理员组do
     */
    private Optional<LdapGroupDO> getAdminGroupDo() {
        return ldapGroupRepo.findByCommonName(EvaLdapUtils.evaLdapProperties.getAdminGroupCn());
    }
}
