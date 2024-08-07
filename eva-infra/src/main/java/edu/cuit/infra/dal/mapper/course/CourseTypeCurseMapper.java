package edu.cuit.infra.dal.mapper.course;

import com.github.yulichang.base.MPJBaseMapper;
import edu.cuit.infra.dal.po.course.CourseTypeCurseDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author XiaoMo
* @description 针对表【course_type_curse(课程类型和一门课程的关联表)】的数据库操作Mapper
* @createDate 2024-08-07 16:48:58
* @Entity edu.cuit.infra.dal.po.course.CourseTypeCurseDO
*/
@Mapper
public interface CourseTypeCurseMapper extends MPJBaseMapper<CourseTypeCurseDO> {

}




