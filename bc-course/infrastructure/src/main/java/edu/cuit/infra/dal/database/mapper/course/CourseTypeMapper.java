package edu.cuit.infra.dal.database.mapper.course;

import com.github.yulichang.base.MPJBaseMapper;
import edu.cuit.infra.dal.database.dataobject.course.CourseTypeDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author XiaoMo
* @description 针对表【course_type(课程类型)】的数据库操作Mapper
* @createDate 2024-08-12 16:50:44
* @Entity edu.cuit.infra.dal.database.dataobject.course.CourseTypeDO
*/
@Mapper
public interface CourseTypeMapper extends MPJBaseMapper<CourseTypeDO> {

}




