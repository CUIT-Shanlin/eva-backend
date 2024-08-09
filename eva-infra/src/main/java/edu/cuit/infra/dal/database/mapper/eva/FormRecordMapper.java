package edu.cuit.infra.dal.database.mapper.eva;

import com.github.yulichang.base.MPJBaseMapper;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
* @author XiaoMo
* @description 针对表【form_record(评教表单记录)】的数据库操作Mapper
* @createDate 2024-08-07 16:53:09
* @Entity edu.cuit.infra.dal.po.eva.FormRecordDO
*/
@Mapper
public interface FormRecordMapper extends MPJBaseMapper<FormRecordDO> {

}




