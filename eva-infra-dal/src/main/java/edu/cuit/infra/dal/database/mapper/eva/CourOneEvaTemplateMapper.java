package edu.cuit.infra.dal.database.mapper.eva;

import com.github.yulichang.base.MPJBaseMapper;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author XiaoMo
* @description 针对表【cour_one_eva_template(课程评教模板的快照，一条数据表示该学期这门课的模板及其统计信息)】的数据库操作Mapper
* @createDate 2024-08-23 14:08:08
* @Entity edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO
*/
@Mapper
public interface CourOneEvaTemplateMapper extends MPJBaseMapper<CourOneEvaTemplateDO> {

}




