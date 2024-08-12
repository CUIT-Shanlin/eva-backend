package edu.cuit.infra.dal.database.mapper.log;

import com.github.yulichang.base.MPJBaseMapper;
import edu.cuit.infra.dal.database.dataobject.log.SysLogDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author XiaoMo
* @description 针对表【sys_log(系统日志表)】的数据库操作Mapper
* @createDate 2024-08-12 16:53:52
* @Entity edu.cuit.infra.dal.database.dataobject.log.SysLogDO
*/
@Mapper
public interface SysLogMapper extends MPJBaseMapper<SysLogDO> {

}




