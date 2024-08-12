package edu.cuit.infra.dal.database.mapper.eva;

import com.github.yulichang.base.MPJBaseMapper;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author XiaoMo
* @description 针对表【form_template(评教表单模板)】的数据库操作Mapper
* @createDate 2024-08-12 16:53:05
* @Entity edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO
*/
@Mapper
public interface FormTemplateMapper extends MPJBaseMapper<FormTemplateDO> {

}




