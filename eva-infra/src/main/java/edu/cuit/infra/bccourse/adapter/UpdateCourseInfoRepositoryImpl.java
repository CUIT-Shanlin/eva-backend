package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.model.UpdateCourseInfoCommand;
import edu.cuit.bc.course.application.port.UpdateCourseInfoRepository;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import edu.cuit.infra.convertor.course.CourseConvertor;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseTypeCourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseTypeCourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.enums.cache.ClassroomCacheConstants;
import edu.cuit.infra.enums.cache.CourseCacheConstants;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * bc-course：修改课程信息端口适配器（复用现有表结构与规则，行为保持不变）。
 */
@Component
@RequiredArgsConstructor
public class UpdateCourseInfoRepositoryImpl implements UpdateCourseInfoRepository {
    private final CourseConvertor courseConvertor;
    private final CourseMapper courseMapper;
    private final SubjectMapper subjectMapper;
    private final CourseTypeCourseMapper courseTypeCourseMapper;
    private final SysUserMapper userMapper;
    private final LocalCacheManager localCacheManager;
    private final CourseCacheConstants courseCacheConstants;
    private final EvaCacheConstants evaCacheConstants;
    private final ClassroomCacheConstants classroomCacheConstants;

    @Override
    @Transactional
    public Map<String, Map<Integer, Integer>> update(UpdateCourseInfoCommand command) {
        Integer semId = command.semesterId();
        UpdateCourseCmd updateCourseCmd = command.updateCourseCmd();

        List<Integer> courseIdList = new ArrayList<>();
        // 先查出课程表中的 subjectId
        CourseDO courseDO = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", updateCourseCmd.getId()));
        if (courseDO == null) {
            throw new QueryException("没有该课程");
        }
        SysUserDO userDO = userMapper.selectById(courseDO.getTeacherId());

        if (Boolean.TRUE.equals(updateCourseCmd.getIsUpdate())) {
            Integer subjectId = courseDO.getSubjectId();
            // 再根据 subjectId 更新对应科目表
            subjectMapper.update(
                    courseConvertor.toSubjectDO(updateCourseCmd.getSubjectMsg()),
                    new QueryWrapper<SubjectDO>().eq("id", subjectId)
            );
            courseIdList.add(courseDO.getId());
            localCacheManager.invalidateCache(null, courseCacheConstants.SUBJECT_LIST);
        } else {
            courseIdList.add(updateCourseCmd.getId());
            SubjectDO subjectDO = subjectMapper.selectById(courseDO.getSubjectId());
            if (!subjectDO.getName().equals(updateCourseCmd.getSubjectMsg().getName())
                    || !Objects.equals(subjectDO.getNature(), updateCourseCmd.getSubjectMsg().getNature())) {
                SubjectDO sujectDo = new SubjectDO();
                sujectDo.setName(updateCourseCmd.getSubjectMsg().getName());
                sujectDo.setNature(updateCourseCmd.getSubjectMsg().getNature());
                sujectDo.setUpdateTime(LocalDateTime.now());
                sujectDo.setCreateTime(LocalDateTime.now());
                subjectMapper.insert(sujectDo);
                courseDO.setSubjectId(sujectDo.getId());
                // 查看是否要删除 subject
                if (courseMapper.selectCount(new QueryWrapper<CourseDO>().eq("subject_id", subjectDO.getId())) == 1) {
                    subjectMapper.delete(new QueryWrapper<SubjectDO>().eq("id", subjectDO.getId()));
                }
                localCacheManager.invalidateCache(null, courseCacheConstants.SUBJECT_LIST);
            }
        }

        List<Integer> typeIds = courseTypeCourseMapper.selectList(
                        new QueryWrapper<CourseTypeCourseDO>().eq("course_id", updateCourseCmd.getId())
                ).stream()
                .map(CourseTypeCourseDO::getTypeId)
                .toList();

        // 判断 typeIds 是否与 typeIdList 一致
        boolean isEq = !typeIds.equals(updateCourseCmd.getTypeIdList());

        // 更新课程表的 templateId 字段
        CourseDO courseDO1 = new CourseDO();
        courseDO1.setTemplateId(updateCourseCmd.getTemplateId());
        courseDO1.setSubjectId(courseDO.getSubjectId());

        for (Integer i : courseIdList) {
            if (isEq) {
                // 先删除，再添加
                courseTypeCourseMapper.delete(new QueryWrapper<CourseTypeCourseDO>().eq("course_id", i));
                // 更新课程类型快照表
                for (Integer typeId : updateCourseCmd.getTypeIdList()) {
                    CourseTypeCourseDO courseTypeCourseDO = new CourseTypeCourseDO();
                    courseTypeCourseDO.setCourseId(updateCourseCmd.getId());
                    courseTypeCourseDO.setTypeId(typeId);
                    courseTypeCourseDO.setCreateTime(LocalDateTime.now());
                    courseTypeCourseDO.setUpdateTime(LocalDateTime.now());
                    courseTypeCourseMapper.insert(courseTypeCourseDO);
                }
            }
            // 更新课程表的 templateId 字段
            courseMapper.update(courseDO1, new QueryWrapper<CourseDO>().eq("id", i));
        }

        Map<String, Map<Integer, Integer>> map = new HashMap<>();
        map.put(userDO.getName() + "的" + updateCourseCmd.getSubjectMsg().getName() + "课程的信息被修改了", null);

        // 清缓存
        localCacheManager.invalidateCache(courseCacheConstants.COURSE_LIST_BY_SEM, String.valueOf(semId));
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId));
        localCacheManager.invalidateCache(null, classroomCacheConstants.ALL_CLASSROOM);
        return map;
    }
}

