package edu.cuit.infra.dal.mapper.eva;

import com.github.yulichang.base.MPJBaseMapper;
import edu.cuit.infra.dal.po.eva.EvaTaskDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author XiaoMo
* @description 针对表【eva_task(评教任务)】的数据库操作Mapper
* @createDate 2024-08-07 16:53:09
* @Entity edu.cuit.infra.dal.po.eva.EvaTaskDO
*/
@Mapper
public interface EvaTaskMapper extends MPJBaseMapper<EvaTaskDO> {

}




