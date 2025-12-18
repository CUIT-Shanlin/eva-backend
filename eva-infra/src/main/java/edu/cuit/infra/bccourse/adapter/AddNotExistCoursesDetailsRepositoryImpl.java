package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.AddNotExistCoursesDetailsRepository;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import edu.cuit.infra.convertor.course.CourseConvertor;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseTypeCourseDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseTypeDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseTypeCourseMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseTypeMapper;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import edu.cuit.infra.enums.cache.ClassroomCacheConstants;
import edu.cuit.infra.enums.cache.CourseCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * bc-course：批量新建多节课（新课程）端口适配器（复用现有表结构与规则，行为保持不变）。
 */
@Component
@RequiredArgsConstructor
public class AddNotExistCoursesDetailsRepositoryImpl implements AddNotExistCoursesDetailsRepository {
    private final CourseConvertor courseConvertor;
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final CourseTypeCourseMapper courseTypeCourseMapper;
    private final CourseTypeMapper courseTypeMapper;
    private final SubjectMapper subjectMapper;
    private final LocalCacheManager localCacheManager;
    private final CourseCacheConstants courseCacheConstants;
    private final FormTemplateMapper formTemplateMapper;
    private final ClassroomCacheConstants classroomCacheConstants;

    @Override
    @Transactional
    public void add(Integer semId, Integer teacherId, UpdateCourseCmd courseInfo, List<SelfTeachCourseTimeCO> dateArr) {
        SubjectDO subjectDO1 = subjectMapper.selectOne(new QueryWrapper<SubjectDO>()
                .eq("name", courseInfo.getSubjectMsg().getName())
                .eq("nature", courseInfo.getSubjectMsg().getNature()));
        Integer subjectId = null;
        if (subjectDO1 == null) {
            SubjectDO subjectDO = courseConvertor.toSubjectDO(courseInfo.getSubjectMsg());
            //向subject表插入数据并返回主键ID
            subjectMapper.insert(subjectDO);
            subjectId = subjectDO.getId();
            localCacheManager.invalidateCache(null, courseCacheConstants.SUBJECT_LIST);
        } else {
            subjectId = subjectDO1.getId();
        }
        //向course表中插入数据
        CourseDO courseDO = courseConvertor.toCourseDO(courseInfo, subjectId, teacherId, semId);
        Integer type = null;
        if (courseDO.getTemplateId() == null && (courseInfo.getSubjectMsg().getNature() == 1 || courseInfo.getSubjectMsg().getNature() == 0)) {
            Integer id = formTemplateMapper.selectOne(new QueryWrapper<FormTemplateDO>().eq("is_default", courseInfo.getSubjectMsg().getNature())).getId();
            courseDO.setTemplateId(id);
            type = courseTypeMapper.selectOne(new QueryWrapper<CourseTypeDO>().eq("is_default", courseInfo.getSubjectMsg().getNature())).getId();
        }
        courseMapper.insert(courseDO);
        localCacheManager.invalidateCache(courseCacheConstants.COURSE_LIST_BY_SEM, String.valueOf(semId));
        //再根据teacherId和subjectId又将他查出来
        Integer courseDOId = courseDO.getId();
        if (type != null && !courseInfo.getTypeIdList().contains(type)) {
            CourseTypeCourseDO courseTypeCourseDO = new CourseTypeCourseDO();
            courseTypeCourseDO.setCourseId(courseDOId);
            courseTypeCourseDO.setTypeId(type);
            courseTypeCourseDO.setCreateTime(courseInfo.getCreateTime());
            courseTypeCourseDO.setUpdateTime(courseInfo.getUpdateTime());
            courseTypeCourseMapper.insert(courseTypeCourseDO);
        }
        //插入课程类型
        for (Integer i : courseInfo.getTypeIdList()) {
            CourseTypeCourseDO courseTypeCourseDO = new CourseTypeCourseDO();
            courseTypeCourseDO.setCourseId(courseDOId);
            courseTypeCourseDO.setTypeId(i);
            courseTypeCourseDO.setCreateTime(courseInfo.getCreateTime());
            courseTypeCourseDO.setUpdateTime(courseInfo.getUpdateTime());
            courseTypeCourseMapper.insert(courseTypeCourseDO);
        }
        //插入课程时间表
        for (SelfTeachCourseTimeCO time : dateArr) {
            for (Integer week : time.getWeeks()) {
                judgeAlsoHasLocation(week, time);
                CourInfDO courInfDO = new CourInfDO();
                courInfDO.setCourseId(courseDOId);
                courInfDO.setWeek(week);
                courInfDO.setDay(time.getDay());
                courInfDO.setStartTime(time.getStartTime());
                courInfDO.setEndTime(time.getEndTime());
                courInfDO.setLocation(time.getClassroom());
                courInfDO.setCreateTime(courseInfo.getCreateTime());
                courInfDO.setUpdateTime(courseInfo.getCreateTime());
                courInfMapper.insert(courInfDO);
            }
        }
        localCacheManager.invalidateCache(null, classroomCacheConstants.ALL_CLASSROOM);

    }

    private void judgeAlsoHasLocation(Integer week, SelfTeachCourseTimeCO timeCO) {
        CourInfDO courInfDO = courInfMapper.selectOne(new QueryWrapper<CourInfDO>()
                .eq("week", week)
                .eq("day", timeCO.getDay())
                .eq("location", timeCO.getClassroom())
                .le("start_time", timeCO.getEndTime())
                .ge("end_time", timeCO.getStartTime()));
        if (courInfDO != null) {
            throw new UpdateException("该时间段教室冲突，请修改时间");
        }

    }
}

