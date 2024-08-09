package edu.cuit.infra.dal.database.mapper.course;

import com.github.yulichang.base.MPJBaseMapper;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import org.apache.ibatis.annotations.Mapper;

/**
* @author XiaoMo
* @description 针对表【course(课程表)】的数据库操作Mapper
* @createDate 2024-08-07 16:48:58
* @Entity edu.cuit.infra.dal.po.course.CourseDO
*/
@Mapper
public interface CourseMapper extends MPJBaseMapper<CourseDO> {

}




