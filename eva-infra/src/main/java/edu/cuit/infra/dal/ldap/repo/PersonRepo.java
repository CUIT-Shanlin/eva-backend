package edu.cuit.infra.dal.ldap.repo;

import edu.cuit.infra.dal.ldap.dataobject.LdapPersonDO;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.stereotype.Repository;

/**
 * Ldap用户操作接口
 */
@Repository
public interface PersonRepo extends LdapRepository<LdapPersonDO> {

    /**
     * 根据用户名获取Ldap用户对象
     * @param username 用户名
     * @return LdapPersonDO
     */
    LdapPersonDO findByUsername(String username);

}
