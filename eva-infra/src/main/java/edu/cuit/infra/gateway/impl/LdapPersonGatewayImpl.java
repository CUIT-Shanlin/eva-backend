package edu.cuit.infra.gateway.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.IdUtil;
import com.alibaba.cola.exception.BizException;
import com.alibaba.cola.exception.SysException;
import edu.cuit.domain.entity.user.LdapPersonEntity;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import edu.cuit.infra.convertor.user.LdapUserConvertor;
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

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LdapPersonGatewayImpl implements LdapPersonGateway {

    private final LdapTemplate ldapTemplate;
    private final LdapPersonRepo ldapPersonRepo;
    private final LdapGroupRepo ldapGroupRepo;
    private final LdapUserConvertor ldapUserConvertor;

    @Override
    public boolean authenticate(String username, String password) {
        EqualsFilter equalsFilter = new EqualsFilter("uid",username);
        return ldapTemplate.authenticate(LdapConstant.USER_BASE_DN,equalsFilter.encode(),password);
    }

    @Override
    public Optional<LdapPersonEntity> findByUsername(String username) {
        Optional<LdapPersonDO> personOpt = ldapPersonRepo.findByUsername(username);
        return personOpt.map(ldapUserConvertor::ldapPersonDoToLdapPersonEntity);
    }

    @Override
    public List<LdapPersonEntity> findAll() {
        return ldapPersonRepo.findAll().stream().map(ldapUserConvertor::ldapPersonDoToLdapPersonEntity).toList();
    }

    @Override
    public void saveUser(LdapPersonEntity user) {
        LdapPersonDO personDO = ldapPersonRepo.findByUsername(user.getUsername()).orElseThrow(() -> new BizException("该用户不存在"));
        BeanUtil.copyProperties(user,personDO, CopyOptions.create().ignoreNullValue().setOverride(true));
        ldapPersonRepo.save(personDO);
    }

    @Override
    public void changePassword(String username, String newPassword) {
        ModificationItem[] mods = new ModificationItem[1];
        mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,new BasicAttribute("userPassword", newPassword));
        ldapTemplate.modifyAttributes(EvaLdapUtils.getUserLdapNameId(username),mods);
    }

    @Override
    public void createUser(LdapPersonEntity user, String password) {
        LdapPersonDO personDO = ldapUserConvertor.ldapPersonEntityToLdapPersonDO(user);
        personDO.setUserPassword(password);
        personDO.setId(EvaLdapUtils.getUserLdapNameId(personDO.getUsername()));
        personDO.setUidNumber(IdUtil.getSnowflakeNextIdStr());
        personDO.setHomeDirectory("/home/" + user.getUsername());
        ldapTemplate.create(personDO);
    }

    @Override
    public void deleteUser(LdapPersonEntity user) {
        LdapPersonDO personDO = ldapUserConvertor.ldapPersonEntityToLdapPersonDO(user);
        personDO.setId(EvaLdapUtils.getUserLdapNameId(personDO.getUsername()));
        ldapTemplate.delete(personDO);
    }

    @Override
    public void addAdmin(String username) {
        Optional<LdapGroupDO> adminGroupDo = EvaLdapUtils.getAdminGroupDo();
        adminGroupDo.orElseThrow(() -> new SysException("ldap获取不到管理员组"));
        List<String> members = adminGroupDo.get().getMembers();
        if (members.contains(username)) throw new BizException("该用户已经是管理员了");
        members.add(username);
        ldapGroupRepo.save(adminGroupDo.get());
    }

    @Override
    public void removeAdmin(String username) {
        Optional<LdapGroupDO> adminGroupDo = EvaLdapUtils.getAdminGroupDo();
        adminGroupDo.orElseThrow(() -> new SysException("ldap获取不到管理员组"));
        List<String> members = adminGroupDo.get().getMembers();
        if (!members.contains(username)) throw new BizException("该用户本来就不是管理员");
        members.remove(username);
        ldapGroupRepo.save(adminGroupDo.get());
    }
}
