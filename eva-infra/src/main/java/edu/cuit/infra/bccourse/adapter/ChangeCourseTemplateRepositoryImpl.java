package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.ChangeCourseTemplateRepository;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import edu.cuit.infra.enums.cache.CourseCacheConstants;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * bc-course：批量切换课程模板持久化适配器（复用现有 Course/Subject 表结构与缓存）。
 */
@Component
@RequiredArgsConstructor
public class ChangeCourseTemplateRepositoryImpl implements ChangeCourseTemplateRepository {
    private final CourseMapper courseMapper;
    private final SubjectMapper subjectMapper;
    private final LocalCacheManager localCacheManager;
    private final CourseCacheConstants courseCacheConstants;
    private final EvaCacheConstants evaCacheConstants;

    @Override
    public void changeTemplate(Integer semesterId, Integer templateId, List<Integer> courseIdList) {
        for (Integer courseId : courseIdList) {
            CourseDO update = new CourseDO();
            update.setTemplateId(templateId);
            courseMapper.update(update, new QueryWrapper<CourseDO>().eq("id", courseId).eq("semester_id", semesterId));

            CourseDO updated = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", courseId).eq("semester_id", semesterId));
            if (updated == null) {
                throw new QueryException("并未找到相关课程");
            }
            Integer subjectId = updated.getSubjectId();
            String name = subjectMapper.selectById(subjectId).getName();
            LogUtils.logContent(name + "(ID:" + courseId + ")课程模板");
        }

        localCacheManager.invalidateCache(courseCacheConstants.COURSE_LIST_BY_SEM, String.valueOf(semesterId));
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semesterId));
    }
}

