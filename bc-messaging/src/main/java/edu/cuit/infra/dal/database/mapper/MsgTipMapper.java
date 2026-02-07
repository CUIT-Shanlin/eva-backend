package edu.cuit.infra.dal.database.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import edu.cuit.infra.dal.database.dataobject.MsgTipDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author XiaoMo
* @description 针对表【msg_tip(消息表)】的数据库操作Mapper
* @createDate 2024-10-14 20:33:37
* @Entity edu.cuit.infra.dal.database.dataobject.MsgTipDO
*/
@Mapper
public interface MsgTipMapper extends MPJBaseMapper<MsgTipDO> {

}



