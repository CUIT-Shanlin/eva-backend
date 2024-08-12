package edu.cuit.infra.dal.database.mapper.log;

import com.github.yulichang.base.MPJBaseMapper;
import edu.cuit.infra.dal.database.dataobject.log.SysLogModuleDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author XiaoMo
* @description 针对表【sys_log_module(系统日志模块表)】的数据库操作Mapper
* @createDate 2024-08-12 16:53:52
* @Entity edu.cuit.infra.dal.database.dataobject.log.SysLogModuleDO
*/
@Mapper
public interface SysLogModuleMapper extends MPJBaseMapper<SysLogModuleDO> {

}




