package edu.cuit.infra.gateway.impl.course.operate;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.evaluation.application.port.EvaTaskBriefByCourInfIdsDirectQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaTaskDeleteByCourInfIdsPort;
import edu.cuit.bc.evaluation.application.port.FormRecordDeleteByTaskIdsPort;
import edu.cuit.client.bo.CourseExcelBO;
import edu.cuit.client.dto.clientobject.eva.CourseScoreCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskBriefCO;
import edu.cuit.infra.convertor.course.CourseConvertor;
import edu.cuit.infra.dal.database.dataobject.course.*;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.*;
import edu.cuit.infra.enums.cache.CourseCacheConstants;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CourseImportExce {
    private final CourseConvertor courseConvertor;
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final CourseTypeCourseMapper courseTypeCourseMapper;
    private final CourseTypeMapper courseTypeMapper;
    private final SubjectMapper subjectMapper;
    private final EvaTaskBriefByCourInfIdsDirectQueryPort evaTaskBriefByCourInfIdsDirectQueryPort;
    private final EvaTaskDeleteByCourInfIdsPort evaTaskDeleteByCourInfIdsPort;
    private final FormRecordDeleteByTaskIdsPort formRecordDeleteByTaskIdsPort;
    private final Object courOneEvaTemplateMapper;
    private final Object userMapper;
    private final Object formTemplateMapper;
    private final LocalCacheManager cacheManager;
    private final CourseCacheConstants courseCacheConstants;
    private final EvaCacheConstants evaCacheConstants;

    public CourseImportExce(
            CourseConvertor courseConvertor,
            CourInfMapper courInfMapper,
            CourseMapper courseMapper,
            CourseTypeCourseMapper courseTypeCourseMapper,
            CourseTypeMapper courseTypeMapper,
            SubjectMapper subjectMapper,
            EvaTaskBriefByCourInfIdsDirectQueryPort evaTaskBriefByCourInfIdsDirectQueryPort,
            EvaTaskDeleteByCourInfIdsPort evaTaskDeleteByCourInfIdsPort,
            FormRecordDeleteByTaskIdsPort formRecordDeleteByTaskIdsPort,
            @Qualifier("courOneEvaTemplateMapper") Object courOneEvaTemplateMapper,
            @Qualifier("sysUserMapper") Object userMapper,
            @Qualifier("formTemplateMapper") Object formTemplateMapper,
            LocalCacheManager cacheManager,
            CourseCacheConstants courseCacheConstants,
            EvaCacheConstants evaCacheConstants
    ) {
        this.courseConvertor = courseConvertor;
        this.courInfMapper = courInfMapper;
        this.courseMapper = courseMapper;
        this.courseTypeCourseMapper = courseTypeCourseMapper;
        this.courseTypeMapper = courseTypeMapper;
        this.subjectMapper = subjectMapper;
        this.evaTaskBriefByCourInfIdsDirectQueryPort = evaTaskBriefByCourInfIdsDirectQueryPort;
        this.evaTaskDeleteByCourInfIdsPort = evaTaskDeleteByCourInfIdsPort;
        this.formRecordDeleteByTaskIdsPort = formRecordDeleteByTaskIdsPort;
        this.courOneEvaTemplateMapper = courOneEvaTemplateMapper;
        this.userMapper = userMapper;
        this.formTemplateMapper = formTemplateMapper;
        this.cacheManager = cacheManager;
        this.courseCacheConstants = courseCacheConstants;
        this.evaCacheConstants = evaCacheConstants;
    }
    //删除这学期所有的课程
    public Map<Integer,Integer> deleteCourse(Integer semId, Integer type) {
        Map<Integer,Integer> evaTaskIds=new HashMap<>();
        //先找出所有的课程
        List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semId));
        List<Integer> courseIds=new ArrayList<>();
        for (CourseDO courseDO : courseDOS) {
            if(subjectMapper.selectById(courseDO.getSubjectId()).getNature().equals(type)){
                courseIds.add(courseDO.getId());
            }
        }
        if(!courseIds.isEmpty()){
            courseMapper.delete(new QueryWrapper<CourseDO>().eq("semester_id", semId).in("id", courseIds));
            cacheManager.invalidateCache(courseCacheConstants.COURSE_LIST_BY_SEM, String.valueOf(semId));
            //删除每节课
            List<Integer> courInfoIds = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in(true,"course_id", courseIds)).stream().map(CourInfDO::getId).toList();
            courInfMapper.delete(new QueryWrapper<CourInfDO>().in(!courseIds.isEmpty(),"course_id", courseIds));
            //删除课程对应的课程类型关联表
            if(!courseIds.isEmpty()){
                courseTypeCourseMapper.delete(new QueryWrapper<CourseTypeCourseDO>().in(true,"course_id", courseIds));
            }
            //删除评教任务
            List<Integer> courInfIdsToQuery = courInfoIds;
            if (courInfIdsToQuery.isEmpty()) {
                courInfIdsToQuery = courInfMapper.selectList(new QueryWrapper<>())
                        .stream()
                        .map(CourInfDO::getId)
                        .toList();
            }

            List<EvaTaskBriefCO> taskBriefList = evaTaskBriefByCourInfIdsDirectQueryPort
                    .findTaskBriefListByCourInfIds(courInfIdsToQuery);
            taskBriefList.forEach(taskBrief -> evaTaskIds.put(taskBrief.getId(), taskBrief.getTeacherId()));
            if(!taskBriefList.isEmpty()) {
                List<Integer> taskIds = taskBriefList.stream().map(EvaTaskBriefCO::getId).toList();
                evaTaskDeleteByCourInfIdsPort.deleteByCourInfIds(courInfIdsToQuery);
                //删除评教表单记录
                formRecordDeleteByTaskIdsPort.deleteByTaskIds(taskIds);
                //删除评教快照
                invoke(
                        courOneEvaTemplateMapper,
                        "delete",
                        new Class<?>[]{Wrapper.class},
                        new Object[]{new QueryWrapper<>().in(!courseIds.isEmpty(),"course_id", courseIds)}
                );
            }
        }
        cacheManager.invalidateCache(null,evaCacheConstants.LOG_LIST);
        cacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId));
        return evaTaskIds;

    }
    public void addAll( Map<String, List<CourseExcelBO>> courseExce, Integer type,Integer semId){
        for (Map.Entry<String, List<CourseExcelBO>> stringListEntry : courseExce.entrySet()) {
            SubjectDO subjectDO1 = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("name", stringListEntry.getKey()).eq("nature",type));
            Integer id;
            if(subjectDO1==null){
                SubjectDO subjectDO = addSubject(stringListEntry.getKey(), type);
                subjectMapper.insert(subjectDO);
                id=subjectDO.getId();
            }else{
                id=subjectDO1.getId();
            }
            for (CourseExcelBO courseExcelBO : stringListEntry.getValue()) {
/*                if(courseExcelBO.getProfTitle()==null){
                    courseExcelBO.setProfTitle("讲师");
                }*/
                SysUserDO userDO = invoke(
                        userMapper,
                        "selectOne",
                        new Class<?>[]{Wrapper.class},
                        new Object[]{new QueryWrapper<SysUserDO>().eq("name", courseExcelBO.getTeacherName())}
                );/*.eq("prof_title", courseExcelBO.getProfTitle()))*/;
                if(userDO==null){//如果老师不存在就把，课程就放弃
//                    subjectMapper.deleteById(id);
                    continue;
                }
                Integer userId = userDO.getId();

               //评教表单模版id
                Integer evaTemplateId = getEvaTemplateId(type);
                CourseDO courseDO = addCourse(id, userId, semId,evaTemplateId);
                //根据subjectId和teacherId看数据库里是否有该课程
                CourseDO courseDO1 = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("subject_id", courseDO.getSubjectId()).eq("teacher_id", courseDO.getTeacherId()).eq("semester_id", courseDO.getSemesterId()));
                if(courseDO1==null){
                    courseMapper.insert(courseDO);
                    //课程类型课程关联表
                    toInsert(courseDO.getId(), type);
                }else{
                    courseDO=courseDO1;
                }
                for (Integer week : courseExcelBO.getWeeks()) {
                    CourInfDO courInfDO = courseConvertor.toCourInfDO(courseDO.getId(), week, courseExcelBO, LocalDateTime.now());
                    if(!courInfMapper.exists(new QueryWrapper<CourInfDO>().eq("course_id", courInfDO.getCourseId()).eq("week", courInfDO.getWeek()).eq("day", courInfDO.getDay()).eq("start_time", courInfDO.getStartTime()).eq("location",courseExcelBO.getClassroom())))
                        courInfMapper.insert(courInfDO);
                }

            }

        }
    }
    private SubjectDO addSubject(String subjectName, Integer nature) {
        SubjectDO subjectDO=new SubjectDO();
        subjectDO.setName(subjectName);
        subjectDO.setNature(nature);
        subjectDO.setCreateTime(LocalDateTime.now());
        subjectDO.setUpdateTime(LocalDateTime.now());
        return subjectDO;
    }
    private CourseDO addCourse(Integer subjectId, Integer userId, Integer semId, Integer evaTemplateId) {
      CourseDO courseDO=new CourseDO();
      courseDO.setSubjectId(subjectId);
      courseDO.setTeacherId(userId);
      courseDO.setSemesterId(semId);
      courseDO.setTemplateId(evaTemplateId);
      courseDO.setCreateTime(LocalDateTime.now());
      courseDO.setUpdateTime(LocalDateTime.now());
      return courseDO;
    }
    private Integer getEvaTemplateId(Integer type){
        Object isDefault = invoke(
                formTemplateMapper,
                "selectOne",
                new Class<?>[]{Wrapper.class},
                new Object[]{new QueryWrapper<>().eq("is_default", type)}
        );
        if(isDefault==null)throw new UpdateException("还没有对应评教模版");
        return invoke(isDefault, "getId", new Class<?>[0], new Object[0]);//is_default
    }
    private void toInsert(Integer courseId,Integer type){
        //根据type来找到课程类型
        Integer id = courseTypeMapper.selectOne(new QueryWrapper<CourseTypeDO>().eq("is_default", type)).getId();
        //插入课程类型课程关联表
        CourseTypeCourseDO courseTypeCourseDO = new CourseTypeCourseDO();
        courseTypeCourseDO.setCourseId(courseId);
        courseTypeCourseDO.setTypeId(id);
        courseTypeCourseDO.setCreateTime(LocalDateTime.now());
        courseTypeCourseDO.setUpdateTime(LocalDateTime.now());
        courseTypeCourseMapper.insert(courseTypeCourseDO);
    }
    public List<CourseScoreCO> getCourseScore(Integer templateId){
         List<CourseScoreCO> result =new ArrayList<>();
        Object formTemplateDO = invoke(
                formTemplateMapper,
                "selectById",
                new Class<?>[]{Serializable.class},
                new Object[]{templateId}
        );
        if(formTemplateDO==null)return result;
        else{
            String props = invoke(formTemplateDO, "getProps", new Class<?>[0], new Object[0]);
            JSONArray jsonArray = JSONUtil.parseArray(props);
            List<String> list = jsonArray.toList(String.class);
            for (String s : list) {
                CourseScoreCO courseScoreCO=new CourseScoreCO();
                courseScoreCO.setProp(s);
                courseScoreCO.setAverScore((double) -1);
                courseScoreCO.setMaxScore((double) -1);
                courseScoreCO.setMinScore((double) -1);
                result.add(courseScoreCO);
            }
            return result;
        }
        
    }

    @SuppressWarnings("unchecked")
    private static <T> T invoke(Object target, String methodName, Class<?>[] paramTypes, Object[] args) {
        try {
            Method method = target.getClass().getMethod(methodName, paramTypes);
            return (T) method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            }
            if (targetException instanceof Error) {
                throw (Error) targetException;
            }
            throw new IllegalStateException(targetException);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
