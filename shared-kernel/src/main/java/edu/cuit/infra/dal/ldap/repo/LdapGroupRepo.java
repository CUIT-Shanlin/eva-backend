package edu.cuit.infra.dal.ldap.repo;

import edu.cuit.infra.dal.ldap.dataobject.LdapGroupDO;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Ldap用户组操作接口
 */
@Repository
public interface LdapGroupRepo extends LdapRepository<LdapGroupDO> {

    /**
     * 根据组名(cnc)查找组
     * @param commonName 组名
     * @return Optional<LdapGroupDO>
     */
    Optional<LdapGroupDO> findByCommonName(String commonName);

}
