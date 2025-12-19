package edu.cuit.infra.bcevaluation.query;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.Week;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.cola.exception.SysException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.cuit.client.dto.clientobject.*;
import edu.cuit.client.dto.clientobject.eva.*;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaLogConditionalQuery;
import edu.cuit.client.dto.query.condition.EvaTaskConditionalQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.client.dto.query.condition.UnqualifiedUserConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.course.CourseEntity;
import edu.cuit.domain.entity.course.SemesterEntity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.course.SubjectEntity;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
import edu.cuit.domain.entity.eva.EvaTemplateEntity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.infra.convertor.PaginationConverter;
import edu.cuit.infra.convertor.course.CourseConvertor;
import edu.cuit.infra.convertor.eva.EvaConvertor;
import edu.cuit.infra.convertor.user.RoleConverter;
import edu.cuit.infra.convertor.user.UserConverter;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import edu.cuit.infra.dal.database.dataobject.user.*;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SemesterMapper;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import edu.cuit.infra.dal.database.mapper.user.*;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.infra.enums.cache.UserCacheConstants;
import edu.cuit.infra.gateway.impl.course.operate.CourseFormat;
import edu.cuit.infra.util.QueryUtils;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;


import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class EvaQueryRepository implements EvaQueryRepo {
    private final CourseMapper courseMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final EvaConvertor evaConvertor;
    private final RoleConverter roleConverter;
    private final PaginationConverter paginationConverter;
    private final UserConverter userConverter;
    private final CourseConvertor courseConvertor;
    private final SysUserMapper sysUserMapper;
    private final SemesterMapper semesterMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SubjectMapper subjectMapper;
    private final CourInfMapper courInfMapper;
    private final FormRecordMapper formRecordMapper;
    private final FormTemplateMapper formTemplateMapper;
    private final CourOneEvaTemplateMapper courOneEvaTemplateMapper;
    private final EvaCacheConstants evaCacheConstants;
    private final LocalCacheManager localCacheManager;
    private final UserCacheConstants userCacheConstants;

    @Override
    public PaginationResultEntity<EvaRecordEntity> pageEvaRecord(Integer semId, PagingQuery<EvaLogConditionalQuery> query) {
        //课程
        Page<FormRecordDO> pageLog=new Page<>(query.getPage(),query.getSize());

        QueryWrapper<CourseDO> courseWrapper=new QueryWrapper<CourseDO>();
        if(CollectionUtil.isNotEmpty(subjectMapper.selectList(new QueryWrapper<SubjectDO>().like(query.getQueryObj().getKeyword()!=null,"name",query.getQueryObj().getKeyword())))){
            List<SubjectDO> subjectDOS=subjectMapper.selectList(new QueryWrapper<SubjectDO>().like("name",query.getQueryObj().getKeyword()));
            List<Integer> subjectIds=subjectDOS.stream().map(SubjectDO::getId).toList();
            if(CollectionUtil.isNotEmpty(subjectIds)){
                courseWrapper.in("subject_id",subjectIds);
            }
        }
        if(CollectionUtil.isEmpty(subjectMapper.selectList(new QueryWrapper<SubjectDO>().like("name",query.getQueryObj().getKeyword())))
                &&query.getQueryObj().getKeyword()!=null){
            List list=new ArrayList();
            return paginationConverter.toPaginationEntity(pageLog,list);
        }
        if(CollectionUtil.isNotEmpty(query.getQueryObj().getCourseTimes())) {
            List<CourInfDO> newCourInfDOS = new ArrayList<>();
            List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(null);
            if(CollectionUtil.isEmpty(formRecordDOS)){
                List list=new ArrayList();
                return paginationConverter.toPaginationEntity(pageLog,list);
            }
            List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>()
                    .in("id",formRecordDOS.stream().map(FormRecordDO::getTaskId).toList()));
            if(CollectionUtil.isEmpty(evaTaskDOS)){
                List list=new ArrayList();
                return paginationConverter.toPaginationEntity(pageLog,list);
            }
            List<CourInfDO> courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>()
                    .in("id",evaTaskDOS.stream().map(EvaTaskDO::getCourInfId).toList()));
            if(CollectionUtil.isEmpty(courInfDOS)){
                List list=new ArrayList();
                return paginationConverter.toPaginationEntity(pageLog,list);
            }
            if (query.getQueryObj().getCourseTimes() != null) {
                for (int i = 0; i < query.getQueryObj().getCourseTimes().size(); i++) {
                    for (int j = 0; j < courInfDOS.size(); j++) {
                        if (courInfDOS.get(j).getWeek() == query.getQueryObj().getCourseTimes().get(i).getWeek()) {
                            if (courInfDOS.get(j).getDay() == query.getQueryObj().getCourseTimes().get(i).getDay()) {
                                if (courInfDOS.get(j).getStartTime() == query.getQueryObj().getCourseTimes().get(i).getStartTime()) {
                                    if (courInfDOS.get(j).getEndTime() == query.getQueryObj().getCourseTimes().get(i).getEndTime()) {
                                        newCourInfDOS.add(courInfDOS.get(j));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            List<Integer> courInfIds=newCourInfDOS.stream().map(CourInfDO::getId).toList();
            if(CollectionUtil.isEmpty(courInfIds)){
                List list=new ArrayList();
                return paginationConverter.toPaginationEntity(pageLog,list);
            }
            List<CourInfDO> courInfDOS1=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("id",courInfIds));
            List<Integer> courseIds1=courInfDOS1.stream().map(CourInfDO::getCourseId).toList();
            if(CollectionUtil.isEmpty(courseIds1)){
                List list=new ArrayList();
                return paginationConverter.toPaginationEntity(pageLog,list);
            }
            courseWrapper.in("id",courseIds1);
        }

        if(query.getQueryObj().getDepartmentName()!=null&&StringUtils.isNotBlank(query.getQueryObj().getDepartmentName())){
            List<Integer> sysUserIds=sysUserMapper.selectList(new QueryWrapper<SysUserDO>().eq(query.getQueryObj().getKeyword()!=null,"department",query.getQueryObj().getDepartmentName()))
                    .stream().map(SysUserDO::getId).toList();
            if(CollectionUtil.isEmpty(sysUserIds)){
                List list=new ArrayList();
                return paginationConverter.toPaginationEntity(pageLog,list);
            }
            courseWrapper.in("teacher_id",sysUserIds);
        }
        if(CollectionUtil.isNotEmpty(query.getQueryObj().getCourseIds())){//课程id变成科目id
            courseWrapper.in("subject_id",query.getQueryObj().getCourseIds());
        }
        if(semId!=null){
            courseWrapper.eq("semester_id",semId);
        }
        if(CollectionUtil.isNotEmpty(query.getQueryObj().getTeacherIds())){
            courseWrapper.in(query.getQueryObj().getKeyword()!=null,"teacher_id",query.getQueryObj().getTeacherIds());
        }
        List<CourseDO> courseDOList=courseMapper.selectList(courseWrapper);

        List<Integer> courseIds=courseDOList.stream().map(CourseDO::getId).toList();
        if(CollectionUtil.isEmpty(courseIds)){
            List list=new ArrayList();
            return paginationConverter.toPaginationEntity(pageLog,list);
        }
        //任务

        QueryWrapper<EvaTaskDO> evaTaskWrapper=new QueryWrapper<EvaTaskDO>();
        List<CourInfDO> courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id",courseIds));
        List<Integer> courseInfoIds=courInfDOS.stream().map(CourInfDO::getId).toList();
        if(CollectionUtil.isEmpty(courseInfoIds)){
            List list=new ArrayList();
            return paginationConverter.toPaginationEntity(pageLog,list);
        }
        evaTaskWrapper.in("cour_inf_id",courseInfoIds);

        if(CollectionUtil.isNotEmpty(query.getQueryObj().getEvaTeacherIds())){
            evaTaskWrapper.in(query.getQueryObj().getEvaTeacherIds()!=null,"teacher_id",query.getQueryObj().getEvaTeacherIds());
        }
        if(query.getQueryObj().getStartEvaluateTime()!=null&&StringUtils.isNotBlank(query.getQueryObj().getStartEvaluateTime())){
            evaTaskWrapper.ge(query.getQueryObj().getStartEvaluateTime()!=null,"create_time",query.getQueryObj().getStartEvaluateTime());
        }

        List<SingleCourseEntity> courseEntities=getListCurInfoEntities(courInfDOS);

        List<EvaTaskDO> evaTaskDOList=evaTaskMapper.selectList(evaTaskWrapper);

        List<Integer> userIds=evaTaskDOList.stream().map(EvaTaskDO::getTeacherId).toList();
        if(CollectionUtil.isEmpty(userIds)){
            List list=new ArrayList();
            return paginationConverter.toPaginationEntity(pageLog,list);
        }
        List<SysUserDO> sysUserDOS=sysUserMapper.selectList(new QueryWrapper<SysUserDO>().in("id",userIds));
        if(CollectionUtil.isEmpty(sysUserDOS)){
            List list=new ArrayList();
            return paginationConverter.toPaginationEntity(pageLog,list);
        }
        List<UserEntity> userEntities=sysUserDOS.stream().map(sysUserDO->toUserEntity(sysUserDO.getId())).toList();

        List<EvaTaskEntity> evaTaskEntities=getEvaTaskEntities(evaTaskDOList,userEntities,courseEntities);

        QueryWrapper<FormRecordDO> formRecordWrapper=new QueryWrapper<FormRecordDO>();
        if(CollectionUtil.isEmpty(evaTaskDOList.stream().map(EvaTaskDO::getId).toList())){
            List list=new ArrayList();
            return paginationConverter.toPaginationEntity(pageLog,list);
        }
        formRecordWrapper.in("task_id",evaTaskDOList.stream().map(EvaTaskDO::getId).toList());

        if(query.getQueryObj().getEndEvaluateTime()!=null&&StringUtils.isNotBlank(query.getQueryObj().getEndEvaluateTime())){
            formRecordWrapper.le(query.getQueryObj().getEndEvaluateTime()!=null,"create_time",query.getQueryObj().getEndEvaluateTime());
        }

        formRecordWrapper.orderByDesc("create_time");
        pageLog = formRecordMapper.selectPage(pageLog,formRecordWrapper);
        List<FormRecordDO> records = pageLog.getRecords();
        List<EvaRecordEntity> list = records.stream().map(formRecordDO->evaConvertor.ToEvaRecordEntity(formRecordDO,
                ()->evaTaskEntities.stream().filter(evaTaskDO->evaTaskDO.getId()
                        .equals(formRecordDO.getTaskId())).findFirst().get())).toList();

        return paginationConverter.toPaginationEntity(pageLog,list);
    }

    @Override
    public PaginationResultEntity<EvaTaskEntity> pageEvaUnfinishedTask(Integer semId, PagingQuery<EvaTaskConditionalQuery> taskQuery) {
        Page<EvaTaskDO> pageTask=new Page<>(taskQuery.getPage(),taskQuery.getSize());
        //再整课程
        List<Integer> courseIds=null;
        QueryWrapper<CourseDO> courseWrapper=new QueryWrapper<CourseDO>();
        if(CollectionUtil.isNotEmpty(subjectMapper.selectList(new QueryWrapper<SubjectDO>().like("name",taskQuery.getQueryObj().getKeyword())))){
            List<SubjectDO> subjectDOS=subjectMapper.selectList(new QueryWrapper<SubjectDO>().like(taskQuery.getQueryObj().getKeyword()!=null,"name",taskQuery.getQueryObj().getKeyword()));
            List<Integer> subjectIds=subjectDOS.stream().map(SubjectDO::getId).toList();
            if(CollectionUtil.isNotEmpty(subjectIds)){
                courseWrapper.in("subject_id",subjectIds);
            }
        }
        if(CollectionUtil.isEmpty(subjectMapper.selectList(new QueryWrapper<SubjectDO>().like("name",taskQuery.getQueryObj().getKeyword())))
                &&taskQuery.getQueryObj().getKeyword()!=null){
            List list=new ArrayList();
            return paginationConverter.toPaginationEntity(pageTask,list);
        }

        if(semId!=null){
            courseWrapper.eq("semester_id",semId);
        }

        List<CourseDO> courseDOList=courseMapper.selectList(courseWrapper);
        courseIds=courseDOList.stream().map(CourseDO::getId).toList();
        if(CollectionUtil.isEmpty(courseIds)){
            List list=new ArrayList();
            return paginationConverter.toPaginationEntity(pageTask,list);
        }

        QueryWrapper<EvaTaskDO> evaTaskWrapper=new QueryWrapper<EvaTaskDO>();

        List<CourInfDO> courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id",courseIds));
        if(CollectionUtil.isEmpty(courInfDOS)){
            List list=new ArrayList();
            return paginationConverter.toPaginationEntity(pageTask,list);
        }
        List<Integer> courseInfoIds=courInfDOS.stream().map(CourInfDO::getId).toList();
        if(CollectionUtil.isEmpty(courseInfoIds)){
            List list=new ArrayList();
            return paginationConverter.toPaginationEntity(pageTask,list);
        }
        evaTaskWrapper.in("cour_inf_id",courseInfoIds);

        List<SingleCourseEntity> courseEntities=getListCurInfoEntities(courInfDOS);
        //未完成的任务
        if(taskQuery.getQueryObj().getTaskStatus()!=null&&taskQuery.getQueryObj().getTaskStatus()>=0) {
            evaTaskWrapper.eq("status", taskQuery.getQueryObj().getTaskStatus());
        }
        QueryUtils.fileCreateTimeQuery(evaTaskWrapper,taskQuery.getQueryObj());

        evaTaskWrapper.orderByDesc("create_time");
        pageTask=evaTaskMapper.selectPage(pageTask,evaTaskWrapper);

        List<SysUserDO> sysUserDOS=sysUserMapper.selectList(null);
        if(CollectionUtil.isEmpty(sysUserDOS)){
            List list=new ArrayList();
            return paginationConverter.toPaginationEntity(pageTask,list);
        }
        List<UserEntity> userEntities=sysUserDOS.stream().map(sysUserDO->toUserEntity(sysUserDO.getId())).toList();

        List<EvaTaskEntity> evaTaskEntities=getEvaTaskEntities(pageTask.getRecords(),userEntities,courseEntities);

        return paginationConverter.toPaginationEntity(pageTask,evaTaskEntities);
    }

    @Override
    public PaginationResultEntity<EvaTemplateEntity> pageEvaTemplate(Integer semId, PagingQuery<GenericConditionalQuery> query) {

        Page<FormTemplateDO> page =new Page<>(query.getPage(),query.getSize());
        QueryWrapper<FormTemplateDO> queryWrapper = new QueryWrapper<>();
        QueryUtils.fileTimeQuery(queryWrapper,query.getQueryObj());
        if(query.getQueryObj().getKeyword()!=null&&StringUtils.isNotBlank(query.getQueryObj().getKeyword())){
            queryWrapper.like(query.getQueryObj().getKeyword()!=null,"name",query.getQueryObj().getKeyword());
        }
        queryWrapper.orderByDesc("create_time");
        Page<FormTemplateDO> formTemplateDOPage = formTemplateMapper.selectPage(page, queryWrapper);
        if(CollectionUtil.isEmpty(formTemplateDOPage.getRecords())){
            List list=new ArrayList();
            return paginationConverter.toPaginationEntity(page,list);
        }

        List<EvaTemplateEntity> evaTemplateEntities=formTemplateDOPage.getRecords().stream().map(pageEvaTemplateDO -> evaConvertor.ToEvaTemplateEntity(pageEvaTemplateDO)).toList();
        return paginationConverter.toPaginationEntity(page,evaTemplateEntities);

    }


    //zjok
    @Override
    public List<EvaTaskEntity> evaSelfTaskInfo(Integer userId,Integer id, String keyword){
        List<CourseDO> courseDOS;
        QueryWrapper<CourseDO> query=new QueryWrapper<CourseDO>();
        if(keyword!=null&&StringUtils.isNotBlank(keyword)) {
            //根据关键字来查询老师
            QueryWrapper<SysUserDO> teacherWrapper = new QueryWrapper<>();
            teacherWrapper.like("name", keyword);
            List<Integer> teacherIds = sysUserMapper.selectList(teacherWrapper).stream().map(SysUserDO::getId).toList();
            //关键字查询课程名称subject->课程->课程详情
            QueryWrapper<SubjectDO> subjectWrapper = new QueryWrapper<>();
            subjectWrapper.like("name", keyword);
            List<Integer> subjectIds = subjectMapper.selectList(subjectWrapper).stream().map(SubjectDO::getId).toList();

            if (CollectionUtil.isNotEmpty(teacherIds) || CollectionUtil.isNotEmpty(subjectIds)) {
                if(CollectionUtil.isNotEmpty(teacherIds) && CollectionUtil.isNotEmpty(subjectIds)){
                    query.in("teacher_id", teacherIds);
                    query.or();
                    query.in("subject_id", subjectIds);
                }else {
                    if (CollectionUtil.isNotEmpty(teacherIds)) {
                        query.in("teacher_id", teacherIds);
                    }
                    if (CollectionUtil.isNotEmpty(subjectIds)) {
                        query.in("subject_id", subjectIds);
                    }
                }
            }else {
                List list=new ArrayList();
                return list;
            }
        }
        if (id != null) {
            query.eq("semester_id", id);
        }
        courseDOS=courseMapper.selectList(query);

        //eva任务->课程详情表->课程表->学期id
        List<Integer> courseIds=courseDOS.stream().map(CourseDO::getId).toList();
        if(CollectionUtil.isEmpty(courseIds)){
            List list=new ArrayList();
            return list;
        }
        List<CourInfDO> courInfDOS;
        if(CollectionUtil.isEmpty(courseIds)){
            List list=new ArrayList();
            return list;
        }else {
            courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id",courseIds));
        }
        List<Integer> courInfIds=courInfDOS.stream().map(CourInfDO::getId).toList();
        if(CollectionUtil.isEmpty(courInfIds)){
            List list=new ArrayList();
            return list;
        }
        List<EvaTaskDO> evaTaskDOS=null;
        if(CollectionUtil.isEmpty(courInfIds)){
            List list=new ArrayList();
            return list;
        }else {
            evaTaskDOS = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id", courInfIds)
                    //顺便选出没有完成的
                    .eq("status", 0).eq("teacher_id", userId));
        }
        if(CollectionUtil.isEmpty(evaTaskDOS)){
            List list=new ArrayList();
            return list;
        }
        List<SingleCourseEntity> courseEntities=getListCurInfoEntities(courInfDOS);
        SysUserDO teacher=sysUserMapper.selectById(userId);
        List<SysUserDO> teachers=new ArrayList<>();
        teachers.add(teacher);

        List<UserEntity> userEntities=teachers.stream().map(sysUserDO->toUserEntity(sysUserDO.getId())).toList();

        return getEvaTaskEntities(evaTaskDOS,userEntities,courseEntities);
    }
    //zjok
    @Override
    public List<EvaRecordEntity> getEvaLogInfo(Integer evaUserId,Integer id,String keyword){
        List<CourseDO> courseDOS;
        QueryWrapper<CourseDO> query=new QueryWrapper<CourseDO>();

        if(keyword!=null&&StringUtils.isNotBlank(keyword)) {
            //根据关键字来查询相关的课程或者老师
            QueryWrapper<SysUserDO> teacherWrapper = new QueryWrapper<>();
            teacherWrapper.like("name", keyword);
            List<Integer> teacherIds = sysUserMapper.selectList(teacherWrapper).stream().map(SysUserDO::getId).toList();
            //关键字查询课程名称subject->课程->课程详情
            QueryWrapper<SubjectDO> subjectWrapper = new QueryWrapper<>();
            subjectWrapper.like("name", keyword);
            List<Integer> subjectIds = subjectMapper.selectList(subjectWrapper).stream().map(SubjectDO::getId).toList();

            if (CollectionUtil.isNotEmpty(teacherIds) || CollectionUtil.isNotEmpty(subjectIds)) {
                if(CollectionUtil.isNotEmpty(teacherIds) && CollectionUtil.isNotEmpty(subjectIds)) {
                    query.in("teacher_id", teacherIds);
                    query.or();
                    query.in("subject_id", subjectIds);
                }else {
                    //评教记录-》评教任务-》课程详情表->课程表->学期id
                    if (CollectionUtil.isNotEmpty(teacherIds)) {
                        query.in("teacher_id", teacherIds);
                    }
                    if (CollectionUtil.isNotEmpty(subjectIds)) {
                        query.in("subject_id", subjectIds);
                    }
                }
            }else{
                List list=new ArrayList();
                return list;
            }
        }
        if(id!=null){
            query.eq("semester_id",id);
        }
        courseDOS=courseMapper.selectList(query);

        //eva任务->课程详情表->课程表->学期id
        List<Integer> courseIds=courseDOS.stream().map(CourseDO::getId).toList();
        if(CollectionUtil.isEmpty(courseIds)){
            List list=new ArrayList();
            return list;
        }
        List<CourInfDO> courInfDOS;
        if(CollectionUtil.isEmpty(courseIds)){
            List list=new ArrayList();
            return list;
        }else {
            courInfDOS = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id", courseIds));
        }
        List<Integer> courInfIds=courInfDOS.stream().map(CourInfDO::getId).toList();
        List<EvaTaskDO> evaTaskDOS;
        if(CollectionUtil.isEmpty(courInfIds)){
            List list=new ArrayList();
            return list;
        }else {
            evaTaskDOS = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id", courInfIds).eq("teacher_id", evaUserId));
        }
        List<Integer> evaTaskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
        List<FormRecordDO> formRecordDOS;
        if(CollectionUtil.isEmpty(evaTaskDOS)){
            List list=new ArrayList();
            return list;
        }else {
            formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIds));
        }

        if(CollectionUtil.isEmpty(formRecordDOS)){
            List list=new ArrayList();
            return list;
        }
        List<SingleCourseEntity> courseEntities=courInfDOS.stream().map(courInfDO -> courseConvertor.toSingleCourseEntity(
                ()->toCourseEntity(courInfDO.getCourseId(),courseMapper.selectById(courInfDO.getCourseId()).getSemesterId()),courInfDO)).toList();

        SysUserDO teacher=sysUserMapper.selectById(evaUserId);
        List<SysUserDO> teachers=new ArrayList<>();
        teachers.add(teacher);

        List<UserEntity> userEntities=teachers.stream().map(sysUserDO->toUserEntity(sysUserDO.getId())).toList();

        List<EvaTaskEntity> evaTaskEntityList=getEvaTaskEntities(evaTaskDOS,userEntities,courseEntities);

        return getRecordEntities(formRecordDOS,evaTaskEntityList);
    }
    //zjok
    @Override
    public List<EvaRecordEntity> getEvaEdLogInfo(Integer userId, Integer semId, Integer courseId) {
        //课程id ->课程->courInfo->evaTask->record
        List<CourInfDO> courInfDOs=new ArrayList<>();
        if(courseId!=null&&courseId>0){
            courInfDOs=courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id",courseId));
        }else{
            QueryWrapper<CourseDO> courseDOQueryWrapper=new QueryWrapper<CourseDO>();
            if(semId!=null){
                courseDOQueryWrapper.eq("semester_id",semId).eq("teacher_id",userId);
            }else{
                courseDOQueryWrapper.eq("teacher_id",userId);
            }
            List<CourseDO> courseDO=courseMapper.selectList(courseDOQueryWrapper);
            List<Integer> couIds=courseDO.stream().map(CourseDO::getId).toList();
            if(CollectionUtil.isEmpty(couIds)){
                List list=new ArrayList();
                return list;
            }else {
                courInfDOs=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id",couIds));
            }
        }
        List<Integer> courInfoIds=courInfDOs.stream().map(CourInfDO::getId).toList();
        if(CollectionUtil.isEmpty(courInfoIds)){
            List list=new ArrayList();
            return list;
        }
        List<EvaTaskDO> evaTaskDOS;
        if(CollectionUtil.isEmpty(courInfoIds)){
            List list=new ArrayList();
            return list;
        }else {
            evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id",courInfoIds));
        }
        List<Integer> evaTaskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
        List<FormRecordDO> formRecordDOS;
        if(CollectionUtil.isEmpty(evaTaskIds)){
            List list=new ArrayList();
            return list;
        }else {
            formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIds));
        }
        //是评教老师，不能用userId
        List<SysUserDO> sysUserDOS;
        List<Integer> userIds=evaTaskDOS.stream().map(EvaTaskDO::getTeacherId).toList();
        if(CollectionUtil.isEmpty(userIds)){
            List list=new ArrayList();
            return list;
        }else {
            sysUserDOS=sysUserMapper.selectList(new QueryWrapper<SysUserDO>().in("id",userIds));
        }
        List<UserEntity> userEntities=sysUserDOS.stream().map(sysUserDO->toUserEntity(sysUserDO.getId())).toList();

        List<SingleCourseEntity> singleCourseEntities=courInfDOs.stream().map(courInfDO -> courseConvertor.toSingleCourseEntity(
                ()->toCourseEntity(courInfDO.getCourseId(),courseMapper.selectById(courInfDO.getCourseId()).getSemesterId()),courInfDO)).toList();
        List<EvaTaskEntity> evaTaskEntities=getEvaTaskEntities(evaTaskDOS,userEntities,singleCourseEntities);

        return getRecordEntities(formRecordDOS,evaTaskEntities);
    }

    //zjok
    @Override
    public Optional<EvaTaskEntity> oneEvaTaskInfo(Integer id) {
        EvaTaskDO getCached=localCacheManager.getCache(evaCacheConstants.ONE_TASK, String.valueOf(id));
        if(getCached==null) {
            if (id == null) {
                throw new QueryException("id为空不能查询");
            }
            EvaTaskDO evaTaskDO = evaTaskMapper.selectOne(new QueryWrapper<EvaTaskDO>().eq("id", id));
            localCacheManager.putCache(evaCacheConstants.ONE_TASK, String.valueOf(id),evaTaskDO);
            getCached=localCacheManager.getCache(evaCacheConstants.ONE_TASK, String.valueOf(id));
            if (evaTaskDO == null) {
                return Optional.empty();
            }
            //老师
            Supplier<UserEntity> teacher = () -> toUserEntity(evaTaskDO.getTeacherId());
            //课程信息
            CourInfDO courInfDO = courInfMapper.selectById(evaTaskDO.getCourInfId());
            if (courInfDO == null) {
                throw new QueryException("并没有找到相关课程信息");
            }
            CourseDO courseDO = courseMapper.selectById(courInfDO.getCourseId());
            Supplier<CourseEntity> course = () -> toCourseEntity(courInfDO.getCourseId(), courseDO.getSemesterId());
            Supplier<SingleCourseEntity> oneCourse = () -> courseConvertor.toSingleCourseEntity(course, courInfDO);

            EvaTaskEntity evaTaskEntity = evaConvertor.ToEvaTaskEntity(evaTaskDO, teacher, oneCourse);
            return Optional.of(evaTaskEntity);
        }else {
            //老师
            EvaTaskDO finalGetCached = getCached;
            Supplier<UserEntity> teacher = () -> toUserEntity(finalGetCached.getTeacherId());
            //课程信息
            CourInfDO courInfDO = courInfMapper.selectById(getCached.getCourInfId());
            if (courInfDO == null) {
                throw new QueryException("并没有找到相关课程信息");
            }
            CourseDO courseDO = courseMapper.selectById(courInfDO.getCourseId());
            Supplier<CourseEntity> course = () -> toCourseEntity(courInfDO.getCourseId(), courseDO.getSemesterId());
            Supplier<SingleCourseEntity> oneCourse = () -> courseConvertor.toSingleCourseEntity(course, courInfDO);

            EvaTaskEntity evaTaskEntity = evaConvertor.ToEvaTaskEntity(getCached, teacher, oneCourse);
            return Optional.of(evaTaskEntity);
        }
    }
//zjok
    @Override
    public Optional<EvaScoreInfoCO> evaScoreStatisticsInfo(Integer semId, Number score) {
        //格式小数，日期
        DecimalFormat df = new DecimalFormat("0.0");
        //学期id->找到课程-》找到课程详情-》评教任务详情-》评教表单记录里面
        List<Integer> evaTaskIdS=getEvaTaskIdS(semId);
        List<FormRecordDO> nowFormRecordDOS;
        if(CollectionUtil.isEmpty(evaTaskIdS)){
            return Optional.empty();
        }else {
            nowFormRecordDOS = formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id", evaTaskIdS));
        }
        Integer totalNum;
        if(CollectionUtil.isEmpty(nowFormRecordDOS)){
            totalNum=0;
        }else {
            //总评教数
            totalNum = nowFormRecordDOS.size();
        }
        //根据他们的form_props_values得到对应的数值
        List<String> strings=nowFormRecordDOS.stream().map(FormRecordDO::getFormPropsValues).toList();
        List<Double> numbers =new ArrayList<>();
        //低于 指定分数的数目
        double aScore = ((BigDecimal) score).doubleValue();
        Integer lowerNum=0;
        Integer higherNum=0;
        for(int i=0;i<strings.size();i++){
            //整个方法把单个text整到平均分
            numbers.add(i, stringToSumAver(strings.get(i)));
            if(aScore>numbers.get(i)){
                lowerNum++;
            }
            if(aScore<numbers.get(i)){
                higherNum++;
            }
        }
        Double percent;
        if(totalNum!=0){
            percent=Double.parseDouble(df.format((higherNum/(double)totalNum)*100.0));
        }else {
            percent = Double.parseDouble(df.format(100));
        }
        //整个方法把以前的数据拿出来
        List<FormRecordDO> last1FormRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).gt("create_time",LocalDateTime.now().minusDays(1)));
        Integer totalNum1;
        if(CollectionUtil.isEmpty(last1FormRecordDOS)){
            totalNum1=0;
        }else {
            totalNum1=last1FormRecordDOS.size();
        }
        //根据他们的form_props_values得到对应的数值
        List<String> strings1=last1FormRecordDOS.stream().map(FormRecordDO::getFormPropsValues).toList();
        List<Double> numbers1=new ArrayList<>();
        //低于 指定分数的数目
        Integer lowerNum1=0;
        Integer higherNum1=0;
        for(int i=0;i<strings1.size();i++){
            //整个方法把单个text整到平均分
            numbers1.add(i, stringToSumAver(strings1.get(i)));
            if(aScore>numbers1.get(i)){
                lowerNum1++;
            }
            if(aScore<numbers1.get(i)){
                higherNum1++;
            }
        }
        Double percent1;
        if(totalNum1!=0) {
            percent1 = Double.parseDouble(df.format((higherNum1 / (double) totalNum1) * 100.0));
        }else {
            percent1 = Double.parseDouble(df.format(100));
        }
        //7日内 percent 的值
        List<SimplePercentCO> percentArr=new ArrayList<>();
        for(int i=0;i<7;i++){
            percentArr.add(getSimplePercent(i,evaTaskIdS,aScore));
        }

        //构建EvaScoreInfoCO对象返回
        EvaScoreInfoCO evaScoreInfoCO=new EvaScoreInfoCO();
        evaScoreInfoCO.setLowerNum(lowerNum);
        evaScoreInfoCO.setTotalNum(totalNum);
        evaScoreInfoCO.setPercent(percent);
        evaScoreInfoCO.setMoreNum(lowerNum-lowerNum1);
        evaScoreInfoCO.setMorePercent(percent-percent1);
        evaScoreInfoCO.setPercentArr(percentArr);
        return Optional.of(evaScoreInfoCO);
    }
    //zjok
    @Override
    public Optional<EvaSituationCO> evaTemplateSituation(Integer semId) {
        //日期格式
        SimpleDateFormat sf=new SimpleDateFormat("YYYY-MM-DD");
        List<EvaTaskDO> evaTaskDOS=new ArrayList<>();
        if(semId!=null){
            List<Integer> evaTaskIds=getEvaTaskIdS(semId);
            if(CollectionUtil.isEmpty(evaTaskIds)){
                return Optional.empty();
            }
            evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("id",evaTaskIds));
        }else{
            evaTaskDOS=evaTaskMapper.selectList(null);
            if(CollectionUtil.isEmpty(evaTaskDOS)){
                return Optional.empty();
            }
        }
        Integer totalNum=0;
        Integer evaNum=0;
        for(int i=0;i<evaTaskDOS.size();i++){
            if(evaTaskDOS.get(i).getStatus()==0){
                totalNum++;
            }
            if(evaTaskDOS.get(i).getStatus()==1){
                evaNum++;
            }
        }
        LocalDateTime end=LocalDateTime.now();
        LocalDateTime start=LocalDateTime.of(end.getYear(),end.getMonthValue(),end.getDayOfMonth(),0,0);

        List<EvaTaskDO> lastEvaTaskDOS=new ArrayList<>();
        if(semId!=null){
            List<Integer> evaTaskIds=getEvaTaskIdS(semId);
            if(CollectionUtil.isEmpty(evaTaskIds)){
                return Optional.empty();
            }
            lastEvaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("id",evaTaskIds).between("create_time",start,end));
        }else{
            lastEvaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().between("create_time",start,end));
            if(CollectionUtil.isEmpty(evaTaskDOS)){
                return Optional.empty();
            }
        }

        Integer unTotalNum=0;
        for(int i=0;i<lastEvaTaskDOS.size();i++){
            if(lastEvaTaskDOS.get(i).getStatus()==0){
                unTotalNum++;
            }
        }
        List<DateEvaNumCO> list=new ArrayList<>();
        for(int i=0;i<7;i++){
            DateEvaNumCO dateEvaNumCO=new DateEvaNumCO();
            dateEvaNumCO.setDate(LocalDate.now().minusDays(i));
            dateEvaNumCO.setValue(getEvaNumByDate(i,semId));
            list.add(dateEvaNumCO);
        }

        EvaSituationCO evaSituationCO=new EvaSituationCO();
        evaSituationCO.setEvaNum(evaNum);
        evaSituationCO.setTotalNum(totalNum);
        evaSituationCO.setMoreNum(getEvaNumByDate(0,semId));
        evaSituationCO.setMoreEvaNum(unTotalNum);
        evaSituationCO.setEvaNumArr(list);

        return Optional.of(evaSituationCO);
    }

    //zjok
    @Override
    public List<Integer> getMonthEvaNUmber(Integer semId) {
        Integer nowYear=LocalDateTime.now().getYear();
        Integer lastYear=LocalDateTime.now().minusMonths(1).getYear();
        //得到这个月
        Integer nowMonth=LocalDateTime.now().getMonthValue();
        Integer lastMonth=LocalDateTime.now().minusMonths(1).getMonthValue();

        LocalDateTime nowStart=LocalDateTime.of(nowYear,nowMonth,1,0,0,0);
        LocalDateTime nowEnd=LocalDateTime.now();
        LocalDateTime lastStart=LocalDateTime.of(lastYear,lastMonth,1,0,0,0);
        LocalDateTime lastEnd=lastStart.plusMonths(1);

        //学期id->课程-》详情-》任务-》记录
        List<Integer> evaTaskIdS=getEvaTaskIdS(semId);

        if(CollectionUtil.isEmpty(evaTaskIdS)){
            List<Integer> list=new ArrayList<>();
            list.add(0);
            list.add(0);
            return list;
        }

        List<FormRecordDO> nowFormRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).between("create_time",nowStart,nowEnd));
        List<FormRecordDO> lastFormRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).between("create_time",lastStart,lastEnd));

        //获取上个月和本月的评教数目，以有两个整数的List形式返回，data[0]：上个月评教数目；data[1]：本月评教数目
        List<Integer> list=new ArrayList<>();
        list.add(lastFormRecordDOS.size());
        list.add(nowFormRecordDOS.size());
        return list;
    }
//zjok
    @Override
    public Optional<EvaWeekAddCO> evaWeekAdd(Integer week,Integer semId) {
        //得到前week周的数据
        LocalDate start=LocalDate.now().minusWeeks(week).with(DayOfWeek.MONDAY);
        LocalDateTime st=LocalDateTime.of(start.getYear(),start.getMonth(),start.getDayOfMonth(),0,0);
        LocalDate end=start.plusDays(7);
        LocalDateTime et=LocalDateTime.of(end.getYear(),end.getMonth(),end.getDayOfMonth(),0,0);

        List<Integer> taskIds=getEvaTaskIdS(semId);
        if(CollectionUtil.isEmpty(taskIds)){
            return Optional.empty();
        }
        Integer moreNum=0;
        List<FormRecordDO> recordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds).between("create_time",st,et));
        if(CollectionUtil.isNotEmpty(recordDOS)){
            moreNum=recordDOS.size();
        }

        Integer lastMoreNum=0;
        List<FormRecordDO> lastRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds).between("create_time",st.minusDays(7),et.minusDays(7)));
        if(CollectionUtil.isNotEmpty(lastRecordDOS)){
            lastMoreNum=lastRecordDOS.size();
        }

        Double percent=0.0;
        if(moreNum!=0&&lastMoreNum!=0) {//TODO
            percent = (moreNum - lastMoreNum) / Double.valueOf(lastMoreNum) * 100;
        }
        List<Integer> weekAdd=new ArrayList<>();
        for(int i=0;i<7;i++){
            LocalDateTime s=st.plusDays(i);
            LocalDateTime e=st.plusDays(i+1);

            Integer num=0;
            List<FormRecordDO> recordDO=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds).between("create_time",s,e));
            if(CollectionUtil.isNotEmpty(recordDO)){
                num=recordDO.size();
            }
            weekAdd.add(i,num);
        }
        return Optional.of(new EvaWeekAddCO().setMoreNum(moreNum).setMorePercent(percent).setEvaNumArr(weekAdd));
    }

    //zjok
    @Override
    public Optional<PastTimeEvaDetailCO> getEvaData(Integer semId, Integer num, Integer target, Integer evaTarget) {
        DecimalFormat ds=new DecimalFormat("0.0");
        SimpleDateFormat sf=new SimpleDateFormat("YY-MM-DD");
        //根据semId找到
        List<Integer> evaTaskIdS=getEvaTaskIdS(semId);

        LocalDateTime timeEnd=LocalDateTime.now();
        LocalDate timeStart=LocalDate.now().minusDays((long)num);

        LocalDate lastStart=LocalDate.now().minusDays((long)2*num);
        LocalDate lastEnd=LocalDate.now().minusDays(num);
        if(CollectionUtil.isEmpty(evaTaskIdS)){
            return Optional.empty();
        }
        List<FormRecordDO> formRecordDOS1=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).between("create_time",timeStart,timeEnd));
        List<FormRecordDO> formRecordDOS2=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).between("create_time",lastStart,lastEnd));
        SimpleEvaPercentCO totalEvaInfo=new SimpleEvaPercentCO();
        totalEvaInfo.setNum(formRecordDOS1.size());
        if(formRecordDOS2.size()!=0) {
            totalEvaInfo.setMorePercent(Double.parseDouble(ds.format((formRecordDOS1.size() / formRecordDOS2.size()) * 100)));
        }else {
            totalEvaInfo.setMorePercent(null);
        }
        List<MoreDateEvaNumCO> dataArr=new ArrayList<>();

        for(int i=1;i<=num;i++){
            List<FormRecordDO> formRecordDOS3;
            if(i==num){
                formRecordDOS3=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).between("create_time",LocalDate.now().minusDays((long)num-i),LocalDateTime.now()));
            }else {
                formRecordDOS3 = formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id", evaTaskIdS).between("create_time", LocalDate.now().minusDays((long) num - i), LocalDate.now().minusDays((long) num - i - 1)));
            }
            MoreDateEvaNumCO dateEvaNumCO=new MoreDateEvaNumCO();
            dateEvaNumCO.setDate(LocalDate.now().minusDays((long)num-i));
            dateEvaNumCO.setMoreEvaNum(formRecordDOS3.size());
            dataArr.add(dateEvaNumCO);
        }
        //SimpleEvaPercentCO evaQualifiedInfo  SimpleEvaPercentCO qualifiedInfo
        List<Integer> getCached=localCacheManager.getCache(null,userCacheConstants.ALL_USER_ID);
        if(CollectionUtil.isEmpty(getCached)) {
            List<SysUserDO> teacher = sysUserMapper.selectList(null);
            List<Integer> teacherIdS = teacher.stream().map(SysUserDO::getId).toList();
            localCacheManager.putCache(null,userCacheConstants.ALL_USER_ID,teacherIdS);
            getCached=localCacheManager.getCache(null,userCacheConstants.ALL_USER_ID);
        }
        if(CollectionUtil.isEmpty(getCached)){
            throw new QueryException("没有找到相关老师");
        }

        Integer evaNum=0;
        Integer pastEvaNum=0;
        Integer evaEdNum=0;
        Integer pastEvaEdNum=0;
        for(int i=0;i<getCached.size();i++){
            Integer n1=getEvaNumByTeacherIdAndLocalTime(getCached.get(i),num,0);
            Integer n2=getEvaEdNumByTeacherIdAndLocalTime(getCached.get(i),num,0);
            Integer m1=getEvaNumByTeacherIdAndLocalTime(getCached.get(i),num*2,num);
            Integer m2=getEvaEdNumByTeacherIdAndLocalTime(getCached.get(i),num*2,num);
            if(n1>=target){
                evaNum++;
            }
            if(n2>=evaTarget){
                evaEdNum++;
            }
            if(m1>=target){
                pastEvaNum++;
            }
            if(m2>=evaTarget){
                pastEvaEdNum++;
            }
        }
        SimpleEvaPercentCO evaQualifiedInfo=new SimpleEvaPercentCO();
        evaQualifiedInfo.setNum(evaNum);
        if(pastEvaNum==0){
            evaQualifiedInfo.setMorePercent(null);
        }else {
            evaQualifiedInfo.setMorePercent(Double.parseDouble(ds.format((evaNum / pastEvaNum) * 100)));
        }
        SimpleEvaPercentCO qualifiedInfo=new SimpleEvaPercentCO();
        qualifiedInfo.setNum(evaEdNum);
        if(pastEvaEdNum==0) {
            qualifiedInfo.setMorePercent(null);
        }else {
            qualifiedInfo.setMorePercent(Double.parseDouble(ds.format((evaEdNum / pastEvaEdNum) * 100)));
        }
        PastTimeEvaDetailCO pastTimeEvaDetailCO=new PastTimeEvaDetailCO();
        pastTimeEvaDetailCO.setTotalEvaInfo(totalEvaInfo);
        pastTimeEvaDetailCO.setEvaQualifiedInfo(evaQualifiedInfo);
        pastTimeEvaDetailCO.setQualifiedInfo(qualifiedInfo);
        pastTimeEvaDetailCO.setDataArr(dataArr);

        return Optional.of(pastTimeEvaDetailCO);
    }
    @Override
    public Optional<UnqualifiedUserResultCO> getEvaTargetAmountUnqualifiedUser(Integer semId, Integer num, Integer target){
        List<Integer> getCached=localCacheManager.getCache(null,userCacheConstants.ALL_USER_ID);
        if(CollectionUtil.isEmpty(getCached)) {
            List<SysUserDO> teacher = sysUserMapper.selectList(null);
            List<Integer> teacherIdS = teacher.stream().map(SysUserDO::getId).toList();
            localCacheManager.putCache(null,userCacheConstants.ALL_USER_ID,teacherIdS);
            getCached=localCacheManager.getCache(null,userCacheConstants.ALL_USER_ID);
        }
        if(CollectionUtil.isEmpty(getCached)){
            throw new QueryException("找不到相关的老师");
        }

        List<UnqualifiedUserInfoCO> dataArr=new ArrayList<>();
        //根据
        for(int i=0;i<getCached.size();i++){
            Integer n=getEvaNumByTeacherId(getCached.get(i),semId);
            if(n<target){
                UnqualifiedUserInfoCO unqualifiedUserInfoCO=new UnqualifiedUserInfoCO();
                unqualifiedUserInfoCO.setDepartment(sysUserMapper.selectById(getCached.get(i)).getDepartment());
                unqualifiedUserInfoCO.setId(getCached.get(i));
                unqualifiedUserInfoCO.setName(sysUserMapper.selectById(getCached.get(i)).getName());
                unqualifiedUserInfoCO.setNum(n);
                dataArr.add(unqualifiedUserInfoCO);
            }
        }
        if(CollectionUtil.isEmpty(dataArr)){
            return Optional.empty();
        }
        //给收集的信息co排个序
        for(int i=0;i<dataArr.size()-1;i++){
            for(int j=i+1;j<dataArr.size();j++){
                if(dataArr.get(i).getNum()>dataArr.get(j).getNum()){
                    UnqualifiedUserInfoCO t=dataArr.get(j);
                    dataArr.set(j,dataArr.get(i));
                    dataArr.set(i,t);
                }
            }
        }
        List<UnqualifiedUserInfoCO> getDataArr=new ArrayList<>();
        for(int i=0;i<num;i++){
            getDataArr.add(i,dataArr.get(i));
        }
        UnqualifiedUserResultCO unqualifiedUserResultCO=new UnqualifiedUserResultCO();
        unqualifiedUserResultCO.setDataArr(getDataArr);
        unqualifiedUserResultCO.setTotal(dataArr.size());
        return Optional.of(unqualifiedUserResultCO);
    }
    @Override
    public Optional<UnqualifiedUserResultCO> getBeEvaTargetAmountUnqualifiedUser(Integer semId,Integer num,Integer target){
        //根据系查老师
        List<Integer> getCached=localCacheManager.getCache(null,userCacheConstants.ALL_USER_ID);
        if(CollectionUtil.isEmpty(getCached)) {
            List<SysUserDO> teacher = sysUserMapper.selectList(null);
            List<Integer> teacherIdS = teacher.stream().map(SysUserDO::getId).toList();
            localCacheManager.putCache(null,userCacheConstants.ALL_USER_ID,teacherIdS);
            getCached=localCacheManager.getCache(null,userCacheConstants.ALL_USER_ID);
        }

        if(CollectionUtil.isEmpty(getCached)){
            throw new QueryException("找不到相关的老师");
        }

        List<UnqualifiedUserInfoCO> dataArr=new ArrayList<>();

        //任务-》课程详情-》课程-》老师
        for(int i=0;i<getCached.size();i++){
            Integer n=getEvaEdNumByTeacherId(getCached.get(i),semId);
            if(n<target){
                UnqualifiedUserInfoCO unqualifiedUserInfoCO=new UnqualifiedUserInfoCO();
                unqualifiedUserInfoCO.setDepartment(sysUserMapper.selectById(getCached.get(i)).getDepartment());
                unqualifiedUserInfoCO.setId(getCached.get(i));
                unqualifiedUserInfoCO.setName(sysUserMapper.selectById(getCached.get(i)).getName());
                unqualifiedUserInfoCO.setNum(n);
                dataArr.add(unqualifiedUserInfoCO);
            }
        }
        if(CollectionUtil.isEmpty(dataArr)){
            return Optional.empty();
        }
        //给收集的信息co排个序
        for(int i=0;i<dataArr.size()-1;i++){
            for(int j=i+1;j<dataArr.size();j++){
                if(dataArr.get(i).getNum()>dataArr.get(j).getNum()){
                    UnqualifiedUserInfoCO t=dataArr.get(j);
                    dataArr.set(j,dataArr.get(i));
                    dataArr.set(i,t);
                }
            }
        }
        List<UnqualifiedUserInfoCO> getDataArr=new ArrayList<>();
        for(int i=0;i<num;i++){
            getDataArr.add(i,dataArr.get(i));
        }
        UnqualifiedUserResultCO unqualifiedUserResultCO=new UnqualifiedUserResultCO();
        unqualifiedUserResultCO.setDataArr(getDataArr);
        unqualifiedUserResultCO.setTotal(dataArr.size());

        return Optional.of(unqualifiedUserResultCO);
    }
    @Override
    public PaginationResultEntity<UnqualifiedUserInfoCO> pageEvaUnqualifiedUserInfo(Integer semId,PagingQuery<UnqualifiedUserConditionalQuery> query, Integer target){
        List<Integer> userIds=new ArrayList<>();
        QueryWrapper<SysUserDO> queryWrapper = new QueryWrapper<>();
        if(query.getQueryObj().getDepartment()!=null&& StringUtils.isNotBlank(query.getQueryObj().getDepartment())){
            queryWrapper.eq("department",query.getQueryObj().getDepartment());
        }
        if(query.getQueryObj().getKeyword()!=null&& StringUtils.isNotBlank(query.getQueryObj().getKeyword())){
            queryWrapper.like("name",query.getQueryObj().getKeyword());
        }

        List<SysUserDO> sysUserDOS=sysUserMapper.selectList(queryWrapper);
        userIds=sysUserDOS.stream().map(SysUserDO::getId).toList();

        if(CollectionUtil.isEmpty(userIds)){
            List list=new ArrayList();
            Page<UnqualifiedUserInfoCO> pageUnqualifiedUserInfoCO=new Page<>(query.getPage(), query.getSize(),0);
            return paginationConverter.toPaginationEntity(pageUnqualifiedUserInfoCO,list);
        }

        List<Integer> teacherIdS=new ArrayList<>();
        List<UnqualifiedUserInfoCO> records=new ArrayList<>();
        for(int i=0;i<userIds.size();i++){
            Integer k=getEvaNumByTeacherId(userIds.get(i),semId);
            if(k<target){
                teacherIdS.add(userIds.get(i));
                UnqualifiedUserInfoCO unqualifiedUserInfoCO=new UnqualifiedUserInfoCO();
                unqualifiedUserInfoCO.setId(userIds.get(i));
                unqualifiedUserInfoCO.setNum(k);
                unqualifiedUserInfoCO.setDepartment(sysUserMapper.selectById(userIds.get(i)).getDepartment());
                unqualifiedUserInfoCO.setName(sysUserMapper.selectById(userIds.get(i)).getName());

                records.add(unqualifiedUserInfoCO);
            }
        }
        List<UnqualifiedUserInfoCO> k=new ArrayList<>();
        for(int i=(query.getPage()-1)*query.getSize();i< query.getPage()* query.getSize();i++){
            if(i>(records.size()-1)){
                break;
            }
            k.add(records.get(i));
        }
        Page<UnqualifiedUserInfoCO> pageUnqualifiedUserInfoCO=new Page<>(query.getPage(), query.getSize(),records.size());

        return paginationConverter.toPaginationEntity(pageUnqualifiedUserInfoCO,k);
    }
    @Override
    public PaginationResultEntity<UnqualifiedUserInfoCO> pageBeEvaUnqualifiedUserInfo(Integer semId,PagingQuery<UnqualifiedUserConditionalQuery> query,Integer target){

        List<Integer> userIds=new ArrayList<>();
        QueryWrapper<SysUserDO> queryWrapper = new QueryWrapper<>();
        if(query.getQueryObj().getDepartment()!=null&& StringUtils.isNotBlank(query.getQueryObj().getDepartment())){
            queryWrapper.eq("department",query.getQueryObj().getDepartment());
        }
        if(query.getQueryObj().getKeyword()!=null&& StringUtils.isNotBlank(query.getQueryObj().getKeyword())){
            queryWrapper.like("name",query.getQueryObj().getKeyword());
        }

        List<SysUserDO> sysUserDOS=sysUserMapper.selectList(queryWrapper);
        userIds=sysUserDOS.stream().map(SysUserDO::getId).toList();
        if(CollectionUtil.isEmpty(userIds)){
            List list=new ArrayList();
            Page<UnqualifiedUserInfoCO> pageUnqualifiedUserInfoCO=new Page<>(query.getPage(), query.getSize(),0);
            return paginationConverter.toPaginationEntity(pageUnqualifiedUserInfoCO,list);
        }

        List<Integer> teacherIdS=new ArrayList<>();
        List<UnqualifiedUserInfoCO> records=new ArrayList<>();
        for(int i=0;i<userIds.size();i++){
            Integer k=getEvaEdNumByTeacherId(userIds.get(i),semId);
            if(k<target){
                teacherIdS.add(userIds.get(i));
                UnqualifiedUserInfoCO unqualifiedUserInfoCO=new UnqualifiedUserInfoCO();
                unqualifiedUserInfoCO.setId(userIds.get(i));
                unqualifiedUserInfoCO.setNum(k);
                unqualifiedUserInfoCO.setDepartment(sysUserMapper.selectById(userIds.get(i)).getDepartment());
                unqualifiedUserInfoCO.setName(sysUserMapper.selectById(userIds.get(i)).getName());

                records.add(unqualifiedUserInfoCO);
            }
        }
        List<UnqualifiedUserInfoCO> k=new ArrayList<>();
        for(int i=(query.getPage()-1)*query.getSize();i< query.getPage()* query.getSize();i++){
            if(i>(records.size()-1)){
                break;
            }
            k.add(records.get(i));
        }
        Page<UnqualifiedUserInfoCO> pageUnqualifiedUserInfoCO=new Page<>(query.getPage(), query.getSize(),records.size());

        return paginationConverter.toPaginationEntity(pageUnqualifiedUserInfoCO,k);
    }

    @Override
    public Optional<Integer> getEvaNumber(Long id) {
        //获取用户已评教数目用户id
        //用户id-》查询评教任务的老师-》查询status==1(已评教)
        QueryWrapper<EvaTaskDO> taskWrapper=new QueryWrapper<EvaTaskDO>().eq("teacher_id",id).eq("status",1);
        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(taskWrapper);
        return Optional.of(evaTaskDOS.size());
    }

//zjok
    @Override
    public Optional<String> getTaskTemplate(Integer taskId, Integer semId) {
        //任务
        List<Integer> evaTaskIdS=getEvaTaskIdS(semId);
        if(CollectionUtil.isEmpty(evaTaskIdS)){
            throw new QueryException("并没有找到相关任务");
        }
        EvaTaskDO evaTaskDO=evaTaskMapper.selectOne(new QueryWrapper<EvaTaskDO>().in("id",evaTaskIdS).eq("id",taskId));
        if(evaTaskDO==null){
            throw new QueryException("无法找到该任务");
        }
        CourInfDO courInfDO=courInfMapper.selectById(evaTaskDO.getCourInfId());
        if(courInfDO==null){
            throw new QueryException("并没有找到相关课程详情");
        }
        //1.直接去快照那边拿到
        CourOneEvaTemplateDO courOneEvaTemplateDO=courOneEvaTemplateMapper.selectOne(new QueryWrapper<CourOneEvaTemplateDO>().eq("course_id",courInfDO.getCourseId()));
        //2.去课程那边拿到
        CourseDO courseDO=courseMapper.selectById(courInfDO.getCourseId());
        FormTemplateDO formTemplateDO=formTemplateMapper.selectOne(new QueryWrapper<FormTemplateDO>().eq("id",courseDO.getTemplateId()));

        if(courOneEvaTemplateDO==null&&formTemplateDO==null){
            throw new QueryException("快照模板和评教模板都没有相关数据");
        }

        if(courOneEvaTemplateDO!=null){
            if(courOneEvaTemplateDO.getFormTemplate()==null){
                return Optional.empty();
            }
            String s1 = CourseFormat.toFormat(courOneEvaTemplateDO.getFormTemplate());
            JSONObject jsonObject= new JSONObject(s1);
            String s=jsonObject.getStr("props");
            return Optional.of(s);
        }else {
            if(formTemplateDO.getProps()==null){
                return Optional.empty();
            }
            return Optional.of(formTemplateDO.getProps());
        }
    }

    @Override
    public List<ScoreRangeCourseCO> scoreRangeCourseInfo(Integer num, Integer interval) {
        //得到全部记录数据
        List<FormRecordDO> getCached=localCacheManager.getCache(null,evaCacheConstants.LOG_LIST);
        if(CollectionUtil.isEmpty(getCached)) {
            List<FormRecordDO> formRecordDOS = formRecordMapper.selectList(null);
            localCacheManager.putCache(null,evaCacheConstants.LOG_LIST,formRecordDOS);
            getCached=localCacheManager.getCache(null,evaCacheConstants.LOG_LIST);
        }
        List<String> strings=getCached.stream().map(FormRecordDO::getFormPropsValues).toList();
        if(CollectionUtil.isEmpty(strings)){
            List list=new ArrayList();
            return list;
        }
        //整到每个记录的分
        List<Double> numbers =new ArrayList<>();
        for(int i=0;i<strings.size();i++){
            //整个方法把单个text整到平均分
            numbers.add(i, stringToSumAver(strings.get(i)));
        }
        List<ScoreRangeCourseCO> scoreRangeCourseCOS = new ArrayList<>();
        for(int i=100;i>100-(num*interval);i=i-interval){
            Integer sum=0;
            for(int j=0;j<numbers.size();j++){
                if(numbers.get(j)<=i&&numbers.get(j)>i-interval){
                    sum++;
                }
            }
            ScoreRangeCourseCO scoreRangeCourseCO=new ScoreRangeCourseCO();
            scoreRangeCourseCO.setStartScore(i-interval);
            scoreRangeCourseCO.setEndScore(i);
            scoreRangeCourseCO.setCount(sum);
            scoreRangeCourseCOS.add(scoreRangeCourseCO);
        }
        return scoreRangeCourseCOS;
    }

    @Override
    public List<EvaTemplateEntity> getAllTemplate() {
        List<FormTemplateDO> getCached=localCacheManager.getCache(null,evaCacheConstants.TEMPLATE_LIST);
        if(CollectionUtil.isEmpty(getCached)) {
            List<FormTemplateDO> formTemplateDOS = formTemplateMapper.selectList(null);
            localCacheManager.putCache(null,evaCacheConstants.TEMPLATE_LIST,formTemplateDOS);
            getCached=localCacheManager.getCache(null,evaCacheConstants.TEMPLATE_LIST);
            if (CollectionUtil.isEmpty(formTemplateDOS)) {
                List list = new ArrayList();
                return list;
            }
            List<EvaTemplateEntity> evaTemplateEntities = formTemplateDOS.stream().map(formTemplateDO -> evaConvertor.ToEvaTemplateEntity(formTemplateDO)).toList();
            return evaTemplateEntities;
        }else {
            return getCached.stream().map(formTemplateDO -> evaConvertor.ToEvaTemplateEntity(formTemplateDO)).toList();
        }
    }

    @Override
    public Optional<Double> getScoreFromRecord(String prop) {
        Double score =stringToSumAver(prop);
        return Optional.of(score);
    }

    @Override
    public Optional<Integer> getEvaNumByCourInfo(Integer courInfId) {
        //通过courInfoId->任务-》记录
        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id",courInfId));
        List<Integer> evaTaskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
        if(CollectionUtil.isEmpty(evaTaskIds)){
            throw new QueryException("并没有找到相关任务");
        }
        List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIds));
        return Optional.of(formRecordDOS.size());
    }

    @Override
    public Optional<Integer> getEvaNumByCourse(Integer courseId) {
        CourseDO courseDO=courseMapper.selectById(courseId);
        List<CourInfDO> courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id",courseDO.getId()));
        List<Integer> courInfoIds=courInfDOS.stream().map(CourInfDO::getId).toList();
        if(CollectionUtil.isEmpty(courInfoIds)){
            return Optional.of(0);
        }
        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id",courInfoIds));
        List<Integer> evaTaskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
        if(CollectionUtil.isEmpty(evaTaskIds)){
            return Optional.of(0);
        }
        List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIds));
        return Optional.of(formRecordDOS.size());
    }

    @Override
    public Optional<String> getNameByTaskId(Integer taskId) {
        return Optional.of(sysUserMapper.selectById(evaTaskMapper.selectById(taskId).getTeacherId()).getName());
    }

    @Override
    public List<EvaRecordEntity> getRecordByCourse(Integer courseId) {
        List<CourInfDO> courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id",courseId));
        if(CollectionUtil.isEmpty(courInfDOS)){
            return List.of();
        }
        List<Integer> courInfoIds=courInfDOS.stream().map(CourInfDO::getId).toList();
        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id",courInfoIds));
        if(CollectionUtil.isEmpty(evaTaskDOS)){
            return List.of();
        }
        List<Integer> evaTaskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
        List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIds));
        if(CollectionUtil.isEmpty(formRecordDOS)){
            return List.of();
        }
        List<UserEntity> userEntities=new ArrayList<>();
        userEntities.add(toUserEntity(courseMapper.selectById(courseId).getTeacherId()));
        List<SingleCourseEntity> singleCourseEntities=getListCurInfoEntities(courInfDOS);
        if(CollectionUtil.isEmpty(singleCourseEntities)) {
            return List.of();
        }
        List<EvaTaskEntity> evaTaskEntities=getEvaTaskEntities(evaTaskDOS,userEntities,singleCourseEntities);
        if(CollectionUtil.isEmpty(evaTaskEntities)) {
            return List.of();
        }
        List<EvaRecordEntity> evaRecordEntities = getRecordEntities(formRecordDOS, evaTaskEntities);
        if(CollectionUtil.isEmpty(evaRecordEntities)){
            return List.of();
        }
        return evaRecordEntities;
    }

    @Override
    public Optional<Double> getScoreByProp(String prop) {
        if(prop==null){
            return Optional.of(-1.0);
        }
        Double score=0.0;
        JSONArray jsonArray;
        try {
            jsonArray = JSONUtil.parseArray(prop, JSONConfig.create()
                    .setIgnoreError(true));
        }catch (Exception e){
            throw new SysException("jsonObject 数据对象转化失败");
        }
        Iterator<Object> iterator = jsonArray.iterator();
        if(jsonArray.size()==0){
            return Optional.of(-1.0);
        }
        while(iterator.hasNext()){
            JSONObject jsonObject = (JSONObject) iterator.next();
            // 处理jsonObject
            score=score+Double.parseDouble(jsonObject.get("score").toString());
        }
        return Optional.of(score/jsonArray.size());
    }

    @Override
    public List<Double> getScoresByProp(String props) {
        if(props==null){
            return List.of();
        }
        Double score=0.0;
        JSONArray jsonArray;
        try {
            jsonArray = JSONUtil.parseArray(props, JSONConfig.create()
                    .setIgnoreError(true));
        }catch (Exception e){
            throw new SysException("jsonObject 数据对象转化失败");
        }
        if(jsonArray.isEmpty()){
            return List.of();
        }
        return jsonArray.stream()
                .map(jsonObject -> Double.parseDouble(((JSONObject) jsonObject).get("score").toString()))
                .toList();
    }

    @Override
    public Map<String, Double> getScorePropMapByProp(String props) {
        if(props==null){
            return Map.of();
        }
        Double score=0.0;
        JSONArray jsonArray;
        try {
            jsonArray = JSONUtil.parseArray(props, JSONConfig.create()
                    .setIgnoreError(true));
        }catch (Exception e){
            throw new SysException("jsonObject 数据对象转化失败");
        }
        if(jsonArray.isEmpty()){
            return Map.of();
        }
        return jsonArray.stream().collect(Collectors
                .toMap(obj -> ((JSONObject) obj).getStr("prop"),obj -> ((JSONObject) obj).getDouble("score")));
    }

    @Override
    public List<Integer> getCountAbEva(Integer semId, Integer userId) {
        List k=new ArrayList();
        k.add(getEvaNumByTeacherId(userId,semId));
        k.add(getEvaEdNumByTeacherId(userId,semId));
        return k;
    }

    //简便方法
    private UserEntity toUserEntity(Integer userId){
        //得到uer对象
        SysUserDO userDO = sysUserMapper.selectById(userId);
        if(userDO==null){
            throw new QueryException("并未找到相关用户");
        }
        //根据userId找到角色id集合
        List<Integer> roleIds = sysUserRoleMapper.selectList(new QueryWrapper<SysUserRoleDO>().eq("user_id", userId)).stream().map(SysUserRoleDO::getRoleId).toList();
        //根据角色id集合找到角色对象集合
        if(roleIds==null){
            throw new QueryException("并没有找到职能");
        }
        Supplier<List<RoleEntity>> roleEntities = ()->sysRoleMapper.selectList(new QueryWrapper<SysRoleDO>().in("id", roleIds)).stream().map(roleDO -> roleConverter.toRoleEntity(roleDO)).toList();
        //根据角色id集合找到角色菜单表中的菜单id集合
        return userConverter.toUserEntity(userDO,roleEntities);
    }
    private CourseEntity toCourseEntity(Integer courseId,Integer semId){
        //构造semester
        Supplier<SemesterEntity> semesterEntity = ()->courseConvertor.toSemesterEntity(semesterMapper.selectById(semId));
        //构造courseDo
        CourseDO courseDO = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", courseId).eq("semester_id", semId));
        if(courseDO==null){
            throw new QueryException("并未找到相关课程");
        }
        //构造subject
        Supplier<SubjectEntity> subjectEntity = ()->courseConvertor.toSubjectEntity(subjectMapper.selectById(courseDO.getSubjectId()));
        //构造userEntity
        Supplier<UserEntity> userEntity =()->toUserEntity(courseMapper.selectById(courseId).getTeacherId());
        return courseConvertor.toCourseEntity(courseDO,subjectEntity,userEntity,semesterEntity);
    }
    //根据传来的String数据form_props_values中的数据解析出来得到平均分
    private Double stringToSumAver(String s) {
        Double score=0.0;
        JSONArray jsonArray;
        try {
            jsonArray = JSONUtil.parseArray(s, JSONConfig.create()
                    .setIgnoreError(true));
        }catch (Exception e){
            throw new SysException("jsonObject 数据对象转化失败");
        }
        Iterator<Object> iterator = jsonArray.iterator();
        while(iterator.hasNext()){
            JSONObject jsonObject = (JSONObject) iterator.next();
            // 处理jsonObject
            score=score+Double.parseDouble(jsonObject.get("score").toString());
        }
        return score/jsonArray.size();
    }
    //根据传来的前n天,还有evaTaskIdS返回SimplePercent对象
    private SimplePercentCO getSimplePercent(Integer n,List<Integer> evaTaskIdS,Double score){
        DecimalFormat df = new DecimalFormat("0.0");
        if(CollectionUtil.isEmpty(evaTaskIdS)){
            SimplePercentCO simplePercentCO=new SimplePercentCO();
            simplePercentCO.setValue(0.0);
            simplePercentCO.setDate(LocalDate.now().minusDays(n));
            return simplePercentCO;
        }
        List<FormRecordDO> lastFormRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).lt("create_time",LocalDateTime.now().minusDays(n)));
        //总评教数
        Integer totalNum=lastFormRecordDOS.size();
        //根据他们的form_props_values得到对应的数值
        List<String> strings=lastFormRecordDOS.stream().map(FormRecordDO::getFormPropsValues).toList();
        if(CollectionUtil.isEmpty(strings)){
            SimplePercentCO simplePercentCO=new SimplePercentCO();
            simplePercentCO.setValue(0.0);
            simplePercentCO.setDate(LocalDate.now().minusDays(n));
            return simplePercentCO;
        }
        List<Double> numbers =new ArrayList<>();
        //低于 指定分数的数目
        Integer higherNum=0;
        for(int i=0;i<strings.size();i++){
            //整个方法把单个text整到平均分
            numbers.add(stringToSumAver(strings.get(i)));
            if(score<numbers.get(i)){
                higherNum++;
            }
        }
        Double percent;
        if(totalNum==0){
            percent=100.0;
        }else {
            percent = Double.parseDouble(df.format((higherNum / (double) totalNum) * 100.0));
        }
        SimplePercentCO simplePercentCO=new SimplePercentCO();
        simplePercentCO.setValue(percent);
        //
        simplePercentCO.setDate(LocalDate.now().minusDays(n));
        return simplePercentCO;
    }
    //根据传来的学期id返回evaTaskIdS
    private List<Integer> getEvaTaskIdS(Integer semId){
        List<EvaTaskDO> getCached=localCacheManager.getCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId));
        if(getCached==null) {
            if (semId == null) {
                List<EvaTaskDO> evaTaskDOS = evaTaskMapper.selectList(null);
                localCacheManager.putCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId),evaTaskDOS);
                getCached=localCacheManager.getCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId));
                if (CollectionUtil.isEmpty(evaTaskDOS)) {
                    return List.of();
                }
                List<Integer> evaTaskIdS = evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
                return evaTaskIdS;
            } else {
                List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semId));

                if (CollectionUtil.isEmpty(courseDOS)) {
                    return List.of();
                }
                List<Integer> courseIdS = courseDOS.stream().map(CourseDO::getId).toList();

                if (CollectionUtil.isEmpty(courseIdS)) {
                    return List.of();
                }

                List<CourInfDO> courInfDOS = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id", courseIdS));
                List<Integer> courInfoIdS = courInfDOS.stream().map(CourInfDO::getId).toList();
                if (CollectionUtil.isEmpty(courInfoIdS)) {
                    return List.of();
                }
                List<EvaTaskDO> evaTaskDOS = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id", courInfoIdS));

                if (CollectionUtil.isEmpty(evaTaskDOS)) {
                    return List.of();
                }
                localCacheManager.putCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId),evaTaskDOS);
                getCached=localCacheManager.getCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId));
                return getCached.stream().map(EvaTaskDO::getId).toList();
            }
        }else {
            return getCached.stream().map(EvaTaskDO::getId).toList();
        }
    }
    //获得几天前的新增评教数
    private Integer getEvaNumByDate(Integer num,Integer semId){
        LocalDateTime start;
        LocalDateTime end=LocalDateTime.now();
        LocalDateTime time=LocalDateTime.of(end.getYear(),end.getMonthValue(),end.getDayOfMonth(),0,0);
        if(num==0){
            end=LocalDateTime.now();
            start=LocalDateTime.of(end.getYear(),end.getMonthValue(),end.getDayOfMonth(),0,0);
        }else{
            end=time.minusDays(num-1);
            start=time.minusDays(num);
        }
        QueryWrapper<FormRecordDO> query=new QueryWrapper<>();
        if(semId!=null){
            List<Integer> evaTaskIds=getEvaTaskIdS(semId);
            if(CollectionUtil.isEmpty(evaTaskIds)){
                return 0;
            }
            query.in("task_id",evaTaskIds);
        }
        List<FormRecordDO> formRecordDOs=formRecordMapper.selectList(query.between("create_time",start,end));
        return formRecordDOs.size();
    }
    //根据evaTaskDOs变成entity数据
    private List<EvaTaskEntity> getEvaTaskEntities(List<EvaTaskDO> evaTaskDOS,List<UserEntity> userEntities,List<SingleCourseEntity> courseEntities){
        List<EvaTaskEntity> evaTaskEntityList=evaTaskDOS.stream().map(evaTaskDO ->evaConvertor.ToEvaTaskEntity(evaTaskDO,
                ()->userEntities.stream().filter(sysUserDO->sysUserDO.getId()
                        .equals(evaTaskDO.getTeacherId())).findFirst().get(),
                ()->courseEntities.stream().filter(courInfDO->courInfDO.getId()
                        .equals(evaTaskDO.getCourInfId())).findFirst().get())).toList();
        if(evaTaskEntityList==null){
            return List.of();
        }
        return evaTaskEntityList;
    }
    private List<EvaRecordEntity> getRecordEntities(List<FormRecordDO> formRecordDOS,List<EvaTaskEntity> evaTaskEntityList){
        List<EvaRecordEntity> list=formRecordDOS.stream().map(formRecordDO -> evaConvertor.ToEvaRecordEntity(formRecordDO,
                (()->evaTaskEntityList.stream().filter(evaTaskDO->evaTaskDO.getId()
                        .equals(formRecordDO.getTaskId())).findFirst().get()))).toList();
        if(list==null){
            throw new QueryException("未找到相关的记录");
        }
        return list;
    }

    //
    private Integer getEvaNumByTeacherId(Integer teacherId,Integer semId){
        List<Integer> evaIds=getEvaTaskIdS(semId);
        if(CollectionUtil.isEmpty(evaIds)){
            return 0;
        }

        List<EvaTaskDO> getCached=localCacheManager.getCache(evaCacheConstants.TASK_LIST_BY_TEACH,sysUserMapper.selectById(teacherId).getName());
        if(CollectionUtil.isEmpty(getCached)) {
            List<EvaTaskDO> evaTaskDOS = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id", teacherId).in("id", evaIds));
            localCacheManager.putCache(evaCacheConstants.TASK_LIST_BY_TEACH,sysUserMapper.selectById(teacherId).getName(),evaTaskDOS);
            getCached=localCacheManager.getCache(evaCacheConstants.TASK_LIST_BY_TEACH,sysUserMapper.selectById(teacherId).getName());
        }
        if(CollectionUtil.isEmpty(getCached)){
            return 0;
        }
        List<Integer> taskIds=getCached.stream().map(EvaTaskDO::getId).toList();
        if(CollectionUtil.isEmpty(taskIds)){
            return 0;
        }
        List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds));
        if(CollectionUtil.isEmpty(formRecordDOS)){
            return 0;
        }
        List<Integer> recordIds=formRecordDOS.stream().map(FormRecordDO::getId).toList();

        return recordIds.size();
    }

    private Integer getEvaEdNumByTeacherId(Integer teacherId,Integer semId){
        List<CourseDO> courseDOS;
        if(semId!=null) {
            courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", teacherId).eq("semester_id",semId));
        }else {
            courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", teacherId));
        }
        if(CollectionUtil.isEmpty(courseDOS)){
            return 0;
        }
        List<Integer> courIdS=courseDOS.stream().map(CourseDO::getId).toList();
        if(CollectionUtil.isEmpty(courIdS)){
            return 0;
        }
        List<CourInfDO> courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id",courIdS));
        List<Integer> courInfoIdS=courInfDOS.stream().map(CourInfDO::getId).toList();
        if(CollectionUtil.isEmpty(courIdS)){
            return 0;
        }
        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id",courInfoIdS));
        List<Integer> taskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();

        if(CollectionUtil.isEmpty(taskIds)){
            return 0;
        }

        List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds));
        if(CollectionUtil.isEmpty(formRecordDOS)){
            return 0;
        }

        List<Integer> recordIds=formRecordDOS.stream().map(FormRecordDO::getId).toList();
        if(CollectionUtil.isEmpty(recordIds)){
            return 0;
        }
        return recordIds.size();
    }
    private Integer getEvaNumByTeacherIdAndLocalTime(Integer teacherId,Integer num1,Integer num2){
        List<EvaTaskDO> getCached=localCacheManager.getCache(evaCacheConstants.TASK_LIST_BY_TEACH,sysUserMapper.selectById(teacherId).getName());
        if(CollectionUtil.isEmpty(getCached)) {
            List<EvaTaskDO> evaTaskDOS = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id", teacherId));
            localCacheManager.putCache(evaCacheConstants.TASK_LIST_BY_TEACH,sysUserMapper.selectById(teacherId).getName(),evaTaskDOS);
            getCached=localCacheManager.getCache(evaCacheConstants.TASK_LIST_BY_TEACH,sysUserMapper.selectById(teacherId).getName());
        }
        List<Integer> taskIds=getCached.stream().map(EvaTaskDO::getId).toList();
        if(CollectionUtil.isEmpty(taskIds)){
            return 0;
        }
        if(num1<num2){
            throw new SysException("你的输入数字num有问题");
        }
        LocalDateTime now=LocalDateTime.now();
        LocalDateTime time=LocalDateTime.of(now.getYear(),now.getMonthValue(),now.getDayOfMonth(),0,0);
        List<FormRecordDO> formRecordDOS;
        if(CollectionUtil.isNotEmpty(taskIds)){
            formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds).between("create_time",time.minusDays(num1),time.minusDays(num2)));
        }else {
            formRecordDOS=null;
        }
        Integer n=0;
        if(formRecordDOS==null){
            n=0;
        }else {
            n=formRecordDOS.size();
        }
        return n;
    }
    private Integer getEvaEdNumByTeacherIdAndLocalTime(Integer teacherId,Integer num1,Integer num2){
        if(num1<num2){
            throw new SysException("你的输入数字num有问题");
        }

        List<CourseDO> courseDOS=courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id",teacherId));
        List<Integer> courIdS=courseDOS.stream().map(CourseDO::getId).toList();

        if(CollectionUtil.isEmpty(courIdS)){
            return 0;
        }else {
            List<CourInfDO> courInfDOS = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id", courIdS));
            List<Integer> courInfoIdS = courInfDOS.stream().map(CourInfDO::getId).toList();
            if(CollectionUtil.isEmpty(courInfoIdS)){
                return 0;
            }
            List<EvaTaskDO> evaTaskDOS = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id", courInfoIdS));
            List<Integer> taskIds = evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
            if(CollectionUtil.isEmpty(taskIds)){
                return 0;
            }
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime time = LocalDateTime.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), 0, 0);

            List<FormRecordDO> formRecordDOS;
            if (CollectionUtil.isNotEmpty(taskIds)) {
                formRecordDOS = formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id", taskIds).between("create_time", time.minusDays(num1), time.minusDays(num2)));
            } else {
                formRecordDOS = null;
            }
            Integer n = 0;
            if (formRecordDOS == null) {
                n = 0;
            } else {
                n = formRecordDOS.size();
            }
            return n;
        }
    }

    private List<SingleCourseEntity> getListCurInfoEntities(List<CourInfDO> courInfDOS){
        return courInfDOS.stream().map(courInfDO ->courseConvertor.toSingleCourseEntity(
                ()->toCourseEntity(courInfDO.getCourseId(),courseMapper.selectById(courInfDO.getCourseId()).getSemesterId()),courInfDO)).toList();
    }

}
