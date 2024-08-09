package edu.cuit.infra.dal.database.mapper.user;

import com.github.yulichang.base.MPJBaseMapper;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import org.apache.ibatis.annotations.Mapper;

/**
* @author XiaoMo
* @description 针对表【sys_role(角色)】的数据库操作Mapper
* @createDate 2024-08-07 16:41:04
* @Entity edu.cuit.infra.dal.po.user.SysRoleDO
*/
@Mapper
public interface SysRoleMapper extends MPJBaseMapper<SysRoleDO> {

}




