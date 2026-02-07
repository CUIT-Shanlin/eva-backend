package edu.cuit.infra.dal.database.mapper.course;

import com.github.yulichang.base.MPJBaseMapper;
import edu.cuit.infra.dal.database.dataobject.course.CourseTypeCourseDO;
import org.apache.ibatis.annotations.Mapper;

/**
* @author XiaoMo
* @description 针对表【course_type_course(课程类型和一门课程的关联表)】的数据库操作Mapper
* @createDate 2024-08-12 16:50:44
* @Entity edu.cuit.infra.dal.database.dataobject.course.CourseTypeCourseDO
*/
@Mapper
public interface CourseTypeCourseMapper extends MPJBaseMapper<CourseTypeCourseDO> {

}




