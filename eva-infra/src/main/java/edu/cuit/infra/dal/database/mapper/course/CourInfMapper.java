package edu.cuit.infra.dal.database.mapper.course;

import com.github.yulichang.base.MPJBaseMapper;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import org.apache.ibatis.annotations.Mapper;

/**
* @author XiaoMo
* @description 针对表【cour_inf(课程详情表)】的数据库操作Mapper
* @createDate 2024-08-07 16:48:58
* @Entity edu.cuit.infra.dal.po.course.CourInfDO
*/
@Mapper
public interface CourInfMapper extends MPJBaseMapper<CourInfDO> {

}




