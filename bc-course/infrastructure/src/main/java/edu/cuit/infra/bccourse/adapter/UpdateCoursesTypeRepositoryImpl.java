package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.model.UpdateCoursesTypeCommand;
import edu.cuit.bc.course.application.port.UpdateCoursesTypeRepository;
import edu.cuit.client.dto.cmd.course.UpdateCoursesToTypeCmd;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseTypeCourseDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseTypeDO;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseTypeCourseMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseTypeMapper;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * bc-course：批量修改课程对应类型端口适配器（复用现有表结构与规则，行为保持不变）。
 */
@Component
@RequiredArgsConstructor
public class UpdateCoursesTypeRepositoryImpl implements UpdateCoursesTypeRepository {
    private final CourseMapper courseMapper;
    private final CourseTypeMapper courseTypeMapper;
    private final CourseTypeCourseMapper courseTypeCourseMapper;

    @Override
    @Transactional
    public void update(UpdateCoursesTypeCommand command) {
        UpdateCoursesToTypeCmd updateCoursesToTypeCmd = command.updateCoursesToTypeCmd();

        List<Integer> courseIdList = updateCoursesToTypeCmd.getCourseIdList();
        if (courseIdList == null || courseIdList.isEmpty()) {
            throw new UpdateException("请选择要更改类型的课程");
        }
        List<Integer> typeIdList = updateCoursesToTypeCmd.getTypeIdList();
        if (typeIdList == null || typeIdList.isEmpty()) {
            throw new UpdateException("请选择要更改的类型");
        }
        for (Integer i : courseIdList) {
            if (!courseMapper.exists(new QueryWrapper<CourseDO>().eq("id", i))) {
                throw new QueryException("该课程已被删除");
            }
            courseTypeCourseMapper.delete(new QueryWrapper<CourseTypeCourseDO>().eq("course_id", i));
            for (Integer integer : typeIdList) {
                if (!courseTypeMapper.exists(new QueryWrapper<CourseTypeDO>().eq("id", integer))) {
                    throw new QueryException("该课程类型那个已被删除");
                }
                CourseTypeCourseDO courseTypeCourseDO = new CourseTypeCourseDO();
                courseTypeCourseDO.setCourseId(i);
                courseTypeCourseDO.setTypeId(integer);
                courseTypeCourseDO.setUpdateTime(LocalDateTime.now());
                courseTypeCourseDO.setCreateTime(LocalDateTime.now());
                courseTypeCourseMapper.insert(courseTypeCourseDO);
            }
        }
    }
}
