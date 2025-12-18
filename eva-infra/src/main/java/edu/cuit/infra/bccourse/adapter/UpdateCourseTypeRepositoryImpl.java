package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.model.UpdateCourseTypeCommand;
import edu.cuit.bc.course.application.port.UpdateCourseTypeRepository;
import edu.cuit.client.dto.cmd.course.UpdateCourseTypeCmd;
import edu.cuit.infra.dal.database.dataobject.course.CourseTypeDO;
import edu.cuit.infra.dal.database.mapper.course.CourseTypeMapper;
import edu.cuit.infra.enums.cache.CourseCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * bc-course：修改一个课程类型端口适配器（复用现有表结构与规则，行为保持不变）。
 */
@Component
@RequiredArgsConstructor
public class UpdateCourseTypeRepositoryImpl implements UpdateCourseTypeRepository {
    private final CourseTypeMapper courseTypeMapper;
    private final LocalCacheManager localCacheManager;
    private final CourseCacheConstants courseCacheConstants;

    @Override
    @Transactional
    public void update(UpdateCourseTypeCommand command) {
        UpdateCourseTypeCmd courseType = command.updateCourseTypeCmd();
        // 判断课程类型是否存在
        CourseTypeDO courseTypeDO = courseTypeMapper.selectById(courseType.getId());
        if (courseTypeDO == null) {
            throw new QueryException("该课程类型不存在");
        }
        // 根据id更新课程类型
        courseTypeDO.setName(courseType.getName());
        courseTypeDO.setDescription(courseType.getDescription());
        courseTypeMapper.update(courseTypeDO, new QueryWrapper<CourseTypeDO>().eq("id", courseType.getId()));
        LogUtils.logContent(courseType.getName() + "课程类型");
        localCacheManager.invalidateCache(null, courseCacheConstants.COURSE_TYPE_LIST);
    }
}

