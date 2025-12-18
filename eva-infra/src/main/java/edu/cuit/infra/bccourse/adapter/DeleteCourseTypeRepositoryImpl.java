package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import edu.cuit.bc.course.application.port.DeleteCourseTypeRepository;
import edu.cuit.infra.dal.database.dataobject.course.CourseTypeCourseDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseTypeDO;
import edu.cuit.infra.dal.database.mapper.course.CourseTypeCourseMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseTypeMapper;
import edu.cuit.infra.enums.cache.CourseCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * bc-course：删除课程类型端口适配器（复用现有表结构与规则，行为保持不变）。
 */
@Component
@RequiredArgsConstructor
public class DeleteCourseTypeRepositoryImpl implements DeleteCourseTypeRepository {
    private final CourseTypeCourseMapper courseTypeCourseMapper;
    private final CourseTypeMapper courseTypeMapper;
    private final LocalCacheManager localCacheManager;
    private final CourseCacheConstants courseCacheConstants;

    @Override
    @Transactional
    public void delete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new UpdateException("请选择要删除的课程类型");
        }
        courseTypeMapper.selectList(new QueryWrapper<CourseTypeDO>().in(true, "id", ids)).forEach(courseTypeDO ->
        {
            if (courseTypeDO.getIsDefault() != -1)
                throw new UpdateException("默认课程类型不能删除");
        });
        QueryWrapper<CourseTypeCourseDO> wrapper = new QueryWrapper<>();
        wrapper.in(!ids.isEmpty(), "type_id", ids);
        //将courseTypeCourse逻辑删除
        courseTypeCourseMapper.delete(wrapper);
        // 将对应课程类型的逻辑删除
        UpdateWrapper<CourseTypeDO> courseTypeWrapper = new UpdateWrapper<>();
        courseTypeWrapper.in(!ids.isEmpty(), "id", ids);
        List<CourseTypeDO> courseTypeDOS = courseTypeMapper.selectList(courseTypeWrapper);
        courseTypeMapper.delete(courseTypeWrapper);
        courseTypeDOS.forEach(courseType -> LogUtils.logContent(courseType.getName() + "课程类型"));
        localCacheManager.invalidateCache(null, courseCacheConstants.COURSE_TYPE_LIST);
    }
}

