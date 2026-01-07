package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.AddCourseTypeRepository;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.infra.dal.database.dataobject.course.CourseTypeDO;
import edu.cuit.infra.dal.database.mapper.course.CourseTypeMapper;
import edu.cuit.infra.enums.cache.CourseCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * bc-course：新增课程类型端口适配器（复用现有表结构与规则，行为保持不变）。
 */
@Component
@RequiredArgsConstructor
public class AddCourseTypeRepositoryImpl implements AddCourseTypeRepository {
    private final CourseTypeMapper courseTypeMapper;
    private final LocalCacheManager localCacheManager;
    private final CourseCacheConstants courseCacheConstants;

    @Override
    @Transactional
    public void add(CourseType courseType) {
        // 根据课程类型名称查询数据库
        CourseTypeDO existingCourseType = courseTypeMapper.selectOne(new QueryWrapper<CourseTypeDO>().eq("name", courseType.getName()));
        if (existingCourseType == null) {
            // 如果不存在相同名称的课程类型，则插入新记录
            CourseTypeDO courseTypeDO = new CourseTypeDO();
            courseTypeDO.setName(courseType.getName());
            courseTypeDO.setDescription(courseType.getDescription());
            courseTypeMapper.insert(courseTypeDO);
            localCacheManager.invalidateCache(null, courseCacheConstants.COURSE_TYPE_LIST);
        } else {
            throw new UpdateException("该课程类型已存在");
        }
    }
}
