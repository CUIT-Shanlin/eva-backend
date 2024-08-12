package edu.cuit.infra.dal.database.mapper.user;

import com.github.yulichang.base.MPJBaseMapper;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author XiaoMo
* @description 针对表【sys_user(用户表)】的数据库操作Mapper
* @createDate 2024-08-12 16:54:41
* @Entity edu.cuit.infra.dal.database.dataobject.user.SysUserDO
*/
@Mapper
public interface SysUserMapper extends MPJBaseMapper<SysUserDO> {

}




