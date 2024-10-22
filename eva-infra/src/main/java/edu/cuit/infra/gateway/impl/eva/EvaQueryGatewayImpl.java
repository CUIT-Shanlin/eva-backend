package edu.cuit.infra.gateway.impl.eva;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.cola.exception.SysException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.cuit.client.dto.clientobject.SimpleEvaPercentCO;
import edu.cuit.client.dto.clientobject.SimplePercentCO;
import edu.cuit.client.dto.clientobject.TimeEvaNumCO;
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
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
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
import edu.cuit.infra.util.QueryUtils;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;


import java.sql.Time;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Supplier;


@Component
@RequiredArgsConstructor
public class EvaQueryGatewayImpl implements EvaQueryGateway {
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

    @Override
    public PaginationResultEntity<EvaRecordEntity> pageEvaRecord(Integer semId, PagingQuery<EvaLogConditionalQuery> query) {
        QueryWrapper userQuery=new QueryWrapper();
        if(query.getQueryObj().getDepartmentName()==null){
            userQuery.eq("department",query.getQueryObj().getDepartmentName());
        }
        if(query.getQueryObj().getKeyword()==null){
            userQuery.like("department",query.getQueryObj().getKeyword());
        }
        List<SysUserDO> sysUserDOList=sysUserMapper.selectList(userQuery);
        List<Integer> sysUserIds=sysUserDOList.stream().map(SysUserDO::getId).toList();
        QueryWrapper<CourseDO> courseWrapper = new QueryWrapper<CourseDO>();
        //先看课程
        if(semId!=null){
            courseWrapper.in("semester_id",semId);
        }
        if(query.getQueryObj().getCourseIds()!=null){
            courseWrapper.in("id",query.getQueryObj().getCourseIds());
        }
        if(query.getQueryObj().getTeacherIds()!=null){
            courseWrapper.in("teacher_id",query.getQueryObj().getTeacherIds());
        }
        List<CourseDO> courseDOS;
        if(sysUserIds==null){
            courseDOS=courseMapper.selectList(null);
        }else {
            courseDOS = courseMapper.selectList(courseWrapper.in("teacher_id", sysUserIds));
        }
        List<Integer> courIds=courseDOS.stream().map(CourseDO::getId).toList();

        //课程详情
        QueryWrapper<CourInfDO> courInfWrapper = new QueryWrapper<CourInfDO>().eq("course_id",courIds);
        List<CourInfDO> courInfDOS=courInfMapper.selectList(courInfWrapper);
        List<CourInfDO> newCourInfDOS=new ArrayList<>();
        if(query.getQueryObj().getCourseTimes()!=null){
            for(int i=0;i<query.getQueryObj().getCourseTimes().size();i++){
                for(int j=0;j<courInfDOS.size();j++){
                    if(courInfDOS.get(j).getWeek()==query.getQueryObj().getCourseTimes().get(i).getWeek()){
                        if(courInfDOS.get(j).getDay()==query.getQueryObj().getCourseTimes().get(i).getDay()){
                            if(courInfDOS.get(j).getStartTime()==query.getQueryObj().getCourseTimes().get(i).getStartTime()){
                                if(courInfDOS.get(j).getEndTime()==query.getQueryObj().getCourseTimes().get(i).getEndTime()){
                                    newCourInfDOS.add(courInfDOS.get(j));
                                }
                            }
                        }
                    }
                }
            }
        }else {
            newCourInfDOS=courInfDOS;
        }
        List<Integer> courInfIds=newCourInfDOS.stream().map(CourInfDO::getId).toList();
        //评教任务
        QueryWrapper<EvaTaskDO> evaTaskWrapper;
        if(courInfIds==null){
            evaTaskWrapper = new QueryWrapper<EvaTaskDO>();
        }else {
            evaTaskWrapper = new QueryWrapper<EvaTaskDO>().in("courInf_id", courInfIds);
        }
        if(query.getQueryObj().getEvaTeacherIds()!=null){
            evaTaskWrapper.in("teacher_id",query.getQueryObj().getTeacherIds());
        }
        if(query.getQueryObj().getStartEvaluateTime()!=null){
            evaTaskWrapper.in("start_time",query.getQueryObj().getStartEvaluateTime());
        }
        List<EvaTaskDO> evaTaskDOS;
        if(sysUserIds==null) {
            evaTaskDOS = evaTaskMapper.selectList(null);
        }else {
            evaTaskDOS = evaTaskMapper.selectList(evaTaskWrapper.in("teacher_id", sysUserIds));
        }
        List<Integer> evaTaskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
        //评教记录
        QueryWrapper<FormRecordDO> formRecordWrapper;
        if(evaTaskIds==null){
            formRecordWrapper=new QueryWrapper<FormRecordDO>();
        }else {
            formRecordWrapper = new QueryWrapper<FormRecordDO>().in("task_id", evaTaskIds);
        }
        if(query.getQueryObj().getEndEvaluateTime()!=null){
            formRecordWrapper.in("start_time",query.getQueryObj().getEndEvaluateTime());
        }
        List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(formRecordWrapper);

        Page<FormRecordDO> pageLog=new Page<>(query.getPage(),query.getSize());
        List<SingleCourseEntity> courseEntities=getListCurInfoEntities(courInfDOS,semId);

        List<Integer> teachers=evaTaskDOS.stream().map(EvaTaskDO::getTeacherId).toList();

        List<SysUserDO> sysUserDOS;
        if(teachers==null){
            sysUserDOS=sysUserMapper.selectList(null);
        }else {
            sysUserDOS=sysUserMapper.selectList(new QueryWrapper<SysUserDO>().in("id",teachers));
        }
        List<UserEntity> userEntities=sysUserDOS.stream().map(teacher->toUserEntity(teacher.getId())).toList();

        List<EvaTaskEntity> evaTaskEntities=getEvaTaskEntities(evaTaskDOS,userEntities,courseEntities);

        pageLog = formRecordMapper.selectPage(pageLog,formRecordWrapper);

        List<FormRecordDO> records = pageLog.getRecords();
        List<EvaRecordEntity> list = records.stream().map(formRecordDO->evaConvertor.ToEvaRecordEntity(formRecordDO,
                ()->evaTaskEntities.stream().filter(evaTaskDO->evaTaskDO.getId()
                        .equals(formRecordDO.getTaskId())).findFirst().get())).toList();
        return paginationConverter.toPaginationEntity(pageLog,list);
    }

    @Override
    public PaginationResultEntity<EvaTaskEntity> pageEvaUnfinishedTask(Integer semId, PagingQuery<EvaTaskConditionalQuery> taskQuery) {
        //先整老师
        List<Integer> userIds=null;
        if(sysUserMapper.selectList(new QueryWrapper<SysUserDO>().like("name",taskQuery.getQueryObj().getKeyword()))!=null) {
            Page<SysUserDO> pageUser=new Page<>(taskQuery.getPage(),taskQuery.getSize());
            pageUser=sysUserMapper.selectPage(pageUser,new QueryWrapper<SysUserDO>().like("name",taskQuery.getQueryObj().getKeyword()));
            userIds=pageUser.getRecords().stream().map(SysUserDO::getId).toList();
        }
        List<SysUserDO> teachers;
        if(userIds==null){
            teachers=sysUserMapper.selectList(null);
        }else {
            teachers=sysUserMapper.selectList(new QueryWrapper<SysUserDO>().in("id",userIds));
        }
        List<UserEntity> userEntities=teachers.stream().map(sysUserDO->toUserEntity(sysUserDO.getId())).toList();
        //再整课程
        List<Integer> courseIds=null;
        Page<CourseDO> pageCourse=new Page<>(taskQuery.getPage(),taskQuery.getSize());
        if(subjectMapper.selectList(new QueryWrapper<SubjectDO>().like("name",taskQuery.getQueryObj().getKeyword()))!=null){

            List<SubjectDO> subjectDOS=subjectMapper.selectList(new QueryWrapper<SubjectDO>().like("name",taskQuery.getQueryObj().getKeyword()));
            List<Integer> subjectIds=subjectDOS.stream().map(SubjectDO::getId).toList();

            if(subjectIds==null){
                pageCourse=courseMapper.selectPage(pageCourse,null);
            }else {
                pageCourse = courseMapper.selectPage(pageCourse, new QueryWrapper<CourseDO>().in("id", subjectIds).eq("semId", semId));
            }
            courseIds=pageCourse.getRecords().stream().map(CourseDO::getId).toList();
        }else {
            if(semId==null){
                pageCourse=courseMapper.selectPage(pageCourse,null);
            }else {
                pageCourse=courseMapper.selectPage(pageCourse,new QueryWrapper<CourseDO>().eq("semId",semId));
            }
            courseIds=pageCourse.getRecords().stream().map(CourseDO::getId).toList();
        }

        Page<EvaTaskDO> pageTask=new Page<>(taskQuery.getPage(),taskQuery.getSize());
        QueryWrapper<EvaTaskDO> evaTaskWrapper=new QueryWrapper<>();

        if(userIds!=null){
            evaTaskWrapper.in("teacher_id",userIds);
        }
        List<CourInfDO> courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id",courseIds));
        List<Integer> courseInfoIds=courInfDOS.stream().map(CourInfDO::getId).toList();
        if(courseIds!=null){
            evaTaskWrapper.in("cour_inf_id",courseInfoIds);
        }
        List<SingleCourseEntity> courseEntities=courInfDOS.stream().map(courInfDO -> courseConvertor.toSingleCourseEntity(
                ()->toCourseEntity(courInfDO.getCourseId(),semId),courInfDO)).toList();
        //未完成的任务
        if(taskQuery.getQueryObj().getTaskStatus()!=null) {
            evaTaskWrapper.eq("status", taskQuery.getQueryObj().getTaskStatus());
        }
        if(taskQuery.getQueryObj().getStartCreateTime()!=null){
            evaTaskWrapper.ge("create_time",taskQuery.getQueryObj().getStartCreateTime());
        }
        if(taskQuery.getQueryObj().getEndCreateTime()!=null){
            evaTaskWrapper.le("create_time",taskQuery.getQueryObj().getEndCreateTime());
        }

        pageTask=evaTaskMapper.selectPage(pageTask,evaTaskWrapper);
        List<EvaTaskDO> records=pageTask.getRecords();

        List<EvaTaskEntity> evaTaskEntities=getEvaTaskEntities(records,userEntities,courseEntities);

        return paginationConverter.toPaginationEntity(pageTask,evaTaskEntities);
    }

    @Override
    public PaginationResultEntity<EvaTemplateEntity> pageEvaTemplate(Integer semId, PagingQuery<GenericConditionalQuery> query) {

        Page<FormTemplateDO> page =new Page<>(query.getPage(),query.getSize());
        QueryWrapper<FormTemplateDO> queryWrapper = new QueryWrapper<>();
        if(query.getQueryObj().getKeyword()!=null){
            queryWrapper.like("name",query.getQueryObj().getKeyword());
        }
        QueryUtils.fileTimeQuery(queryWrapper,query.getQueryObj());
        Page<FormTemplateDO> formTemplateDOPage = formTemplateMapper.selectPage(page, queryWrapper);
        List<EvaTemplateEntity> evaTemplateEntities=formTemplateDOPage.getRecords().stream().map(pageEvaTemplateDO -> evaConvertor.ToEvaTemplateEntity(pageEvaTemplateDO)).toList();
        return paginationConverter.toPaginationEntity(page,evaTemplateEntities);
    }


    //zjok
    @Override
    public List<EvaTaskEntity> evaSelfTaskInfo(Integer userId,Integer id, String keyword){
        //根据关键字来查询老师
        QueryWrapper<SysUserDO> teacherWrapper =new QueryWrapper<>();
        teacherWrapper.like("name",keyword);
        List<Integer> teacherIds=sysUserMapper.selectList(teacherWrapper).stream().map(SysUserDO::getId).toList();
        //关键字查询课程名称subject->课程->课程详情
        QueryWrapper<SubjectDO> subjectWrapper =new QueryWrapper<>();
        subjectWrapper.like("name",keyword);
        List<Integer> subjectIds=subjectMapper.selectList(subjectWrapper).stream().map(SubjectDO::getId).toList();

        if(teacherIds!=null||subjectIds!=null){

            List<CourseDO> courseDOS;
            QueryWrapper<CourseDO> query=new QueryWrapper<CourseDO>();
            if(id!=null){
                query.eq("semester_id",id);
            }
            if(teacherIds!=null){
                query.in("teacher_id",teacherIds);
            }
            if(subjectIds!=null){
                query.in("subject_id",subjectIds);
            }
            courseDOS=courseMapper.selectList(query);

            //eva任务->课程详情表->课程表->学期id
            List<Integer> courseIds=courseDOS.stream().map(CourseDO::getId).toList();
            List<CourInfDO> courInfDOS;
            if(courseIds==null){
                courInfDOS=courInfMapper.selectList(null);
            }else {
                courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id",courseIds));
            }
            List<Integer> courInfIds=courInfDOS.stream().map(CourInfDO::getId).toList();
            List<EvaTaskDO> evaTaskDOS=null;
            if(courInfIds==null){
                evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>()
                        //顺便选出没有完成的
                        .eq("status",0).eq("teacher_id",userId));
            }else {
                evaTaskDOS = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id", courInfIds)
                        //顺便选出没有完成的
                        .eq("status", 0).eq("teacher_id", userId));
            }
            if(evaTaskDOS==null){
                throw new QueryException("并没有找到相关的任务");
            }
            List<SingleCourseEntity> courseEntities=getListCurInfoEntities(courInfDOS,id);

            SysUserDO teacher=sysUserMapper.selectById(userId);
            List<SysUserDO> teachers=new ArrayList<>();
            teachers.add(teacher);

            List<UserEntity> userEntities=teachers.stream().map(sysUserDO->toUserEntity(sysUserDO.getId())).toList();

            return getEvaTaskEntities(evaTaskDOS,userEntities,courseEntities);
        }else{
            throw new QueryException("你输入的关键字里面并没有相应的老师名称或者课程名称");
        }
    }
    //zjok
    @Override
    public List<EvaRecordEntity> getEvaLogInfo(Integer evaUserId,Integer id,String keyword) {
        //根据关键字来查询相关的课程或者老师
        QueryWrapper<SysUserDO> teacherWrapper =new QueryWrapper<>();
        teacherWrapper.like("name",keyword);
        List<Integer> teacherIds=sysUserMapper.selectList(teacherWrapper).stream().map(SysUserDO::getId).toList();
        //关键字查询课程名称subject->课程->课程详情
        QueryWrapper<SubjectDO> subjectWrapper =new QueryWrapper<>();
        subjectWrapper.like("name",keyword);
        List<Integer> subjectIds=subjectMapper.selectList(subjectWrapper).stream().map(SubjectDO::getId).toList();

        if(teacherIds!=null||subjectIds!=null){
            //评教记录-》评教任务-》课程详情表->课程表->学期id
            List<CourseDO> courseDOS;
            QueryWrapper<CourseDO> query=new QueryWrapper<CourseDO>();
            if(id!=null){
                query.eq("semester_id",id);
            }
            if(teacherIds!=null){
                query.in("teacher_id",teacherIds);
            }
            if(subjectIds!=null){
                query.in("subject_id",subjectIds);
            }
            courseDOS=courseMapper.selectList(query);

            //eva任务->课程详情表->课程表->学期id
            List<Integer> courseIds=courseDOS.stream().map(CourseDO::getId).toList();
            List<CourInfDO> courInfDOS;
            if(courseIds==null){
                courInfDOS=courInfMapper.selectList(null);
            }else {
                courInfDOS = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id", courseIds));
            }
            List<Integer> courInfIds=courInfDOS.stream().map(CourInfDO::getId).toList();
            List<EvaTaskDO> evaTaskDOS;
            if(courInfIds==null){
                evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id",evaUserId));
            }else {
                evaTaskDOS = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id", courInfIds).eq("teacher_id", evaUserId));
            }
            List<Integer> evaTaskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
            List<FormRecordDO> formRecordDOS;
            if(evaTaskIds==null){
                formRecordDOS=formRecordMapper.selectList(null);
            }else {
                formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIds));
            }

            if(formRecordDOS==null){
                throw new QueryException("并没有找到相关的评教记录");
            }
            List<SingleCourseEntity> courseEntities=courInfDOS.stream().map(courInfDO -> courseConvertor.toSingleCourseEntity(
                    ()->toCourseEntity(courInfDO.getCourseId(),id),courInfDO)).toList();

            SysUserDO teacher=sysUserMapper.selectById(evaUserId);
            List<SysUserDO> teachers=new ArrayList<>();
            teachers.add(teacher);

            List<UserEntity> userEntities=teachers.stream().map(sysUserDO->toUserEntity(sysUserDO.getId())).toList();

            List<EvaTaskEntity> evaTaskEntityList=getEvaTaskEntities(evaTaskDOS,userEntities,courseEntities);

            return getRecordEntities(formRecordDOS,evaTaskEntityList);
        }else{
            throw new QueryException("你给的关键词信息不足以查询出应有课程或者老师");
        }
    }
    //zjok
    @Override
    public List<EvaRecordEntity> getEvaEdLogInfo(Integer userId, Integer semId, Integer courseId) {
        //课程id ->课程->courInfo->evaTask->record
        List<CourInfDO> courInfDOs=new ArrayList<>();
        if(courseId!=null&&courseId>=0){
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
            if(couIds==null){
                courInfDOs=courInfMapper.selectList(null);
            }else {
                courInfDOs=courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id",couIds));
            }
        }
        List<Integer> courInfoIds=courInfDOs.stream().map(CourInfDO::getId).toList();
        List<EvaTaskDO> evaTaskDOS;
        if(courInfoIds==null){
            evaTaskDOS=evaTaskMapper.selectList(null);
        }else {
            evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id",courInfoIds));
        }
        List<Integer> evaTaskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
        List<FormRecordDO> formRecordDOS;
        if(evaTaskIds==null){
            formRecordDOS=formRecordMapper.selectList(null);
        }else {
            formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIds));
        }
        //是评教老师，不能用userId
        List<SysUserDO> sysUserDOS;
        List<Integer> userIds=evaTaskDOS.stream().map(EvaTaskDO::getTeacherId).toList();
        if(userIds==null){
            sysUserDOS=sysUserMapper.selectList(null);
        }else {
            sysUserDOS=sysUserMapper.selectList(new QueryWrapper<SysUserDO>().in("id",userIds));
        }
        List<UserEntity> userEntities=sysUserDOS.stream().map(sysUserDO->toUserEntity(sysUserDO.getId())).toList();

        List<SingleCourseEntity> singleCourseEntities=courInfDOs.stream().map(courInfDO -> courseConvertor.toSingleCourseEntity(
                ()->toCourseEntity(courInfDO.getCourseId(),semId),courInfDO)).toList();
        List<EvaTaskEntity> evaTaskEntities=getEvaTaskEntities(evaTaskDOS,userEntities,singleCourseEntities);

        return getRecordEntities(formRecordDOS,evaTaskEntities);
    }

    //zjok
    @Override
    public Optional<EvaTaskEntity> oneEvaTaskInfo(Integer id) {
        if(id==null){
            throw new QueryException("id为空不能查询");
        }
        EvaTaskDO evaTaskDO=evaTaskMapper.selectOne(new QueryWrapper<EvaTaskDO>().eq("id",id));
        if(evaTaskDO==null){
            throw new QueryException("并没有找到相应的任务");
        }
        //老师
        Supplier<UserEntity> teacher=()->toUserEntity(evaTaskDO.getTeacherId());
        //课程信息
        CourInfDO courInfDO=courInfMapper.selectById(evaTaskDO.getCourInfId());
        CourseDO courseDO=courseMapper.selectById(courInfDO.getCourseId());
        Supplier<CourseEntity> course=()->toCourseEntity(courInfDO.getCourseId(),courseDO.getSemesterId());
        Supplier<SingleCourseEntity> oneCourse=()->courseConvertor.toSingleCourseEntity(course,courInfDO);

        EvaTaskEntity evaTaskEntity=evaConvertor.ToEvaTaskEntity(evaTaskDO,teacher,oneCourse);
        return Optional.of(evaTaskEntity);
    }
//zjok
    @Override
    public Optional<EvaScoreInfoCO> evaScoreStatisticsInfo(Integer semId, Number score) {
        //格式小数，日期
        DecimalFormat df = new DecimalFormat("0.0");
        //学期id->找到课程-》找到课程详情-》评教任务详情-》评教表单记录里面
        List<Integer> evaTaskIdS=getEvaTaskIdS(semId);
        List<FormRecordDO> nowFormRecordDOS;
        if(evaTaskIdS==null){
            nowFormRecordDOS=formRecordMapper.selectList(null);
        }else {
            nowFormRecordDOS = formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id", evaTaskIdS));
        }
        if(nowFormRecordDOS==null){
            throw new QueryException("未找到相应的评教记录");
        }
        //总评教数
        Integer totalNum=nowFormRecordDOS.size();
        //根据他们的form_props_values得到对应的数值
        List<String> strings=nowFormRecordDOS.stream().map(FormRecordDO::getTextValue).toList();
        List<Double> numbers =new ArrayList<>();
        //低于 指定分数的数目
        Double aScore=(Double) score;
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
        Double percent=Double.parseDouble(df.format((higherNum/(double)totalNum)*100.0));
        //整个方法把以前的数据拿出来
        List<FormRecordDO> last1FormRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).gt("create_time",LocalDateTime.now().minusDays(1)));
        if(last1FormRecordDOS==null){
            throw new QueryException("未找到相应的评教记录");
        }
        //总评教数
        Integer totalNum1=last1FormRecordDOS.size();
        //根据他们的form_props_values得到对应的数值
        List<String> strings1=last1FormRecordDOS.stream().map(FormRecordDO::getTextValue).toList();
        List<Double> numbers1 =new ArrayList<>();
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
        Double percent1=Double.parseDouble(df.format((higherNum1/(double)totalNum1)*100.0));
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
        SimpleDateFormat sf=new SimpleDateFormat("YY-MM-DD");
        List<EvaTaskDO> evaTaskDOS=new ArrayList<>();
        if(semId!=null){
            List<Integer> evaTaskIds=getEvaTaskIdS(semId);
            evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("task_id",evaTaskIds));
        }else{
            evaTaskDOS=evaTaskMapper.selectList(null);
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
            lastEvaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("task_id",evaTaskIds).between("create_time",start,end));
        }else{
            lastEvaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().between("create_time",start,end));
        }
        Integer unTotalNum=0;
        for(int i=0;i<lastEvaTaskDOS.size();i++){
            if(lastEvaTaskDOS.get(i).getStatus()==0){
                unTotalNum++;
            }
        }
        List<TimeEvaNumCO> list=new ArrayList<>();
        for(int i=0;i<7;i++){
            TimeEvaNumCO timeEvaNumCO=new TimeEvaNumCO();
            timeEvaNumCO.setTime(sf.format(LocalDate.now().minusDays(i)));
            timeEvaNumCO.setMoreEvaNum(getEvaNumByDate(i,semId));
            list.add(timeEvaNumCO);
        }

        EvaSituationCO evaSituationCO=new EvaSituationCO();
        evaSituationCO.setEvaNum(evaNum);
        evaSituationCO.setTotalNum(totalNum);
        evaSituationCO.setMoreNum(getEvaNumByDate(0,semId));
        evaSituationCO.setMoreEvaNum(unTotalNum);
        evaSituationCO.setEvaNumArr(list);

        return Optional.empty();
    }

    //zjok
    @Override
    public List<Integer> getMonthEvaNUmber(Integer semId) {
        Integer nowYear=LocalDateTime.now().getYear();
        Integer lastYear=LocalDateTime.now().minusMonths(1).getYear();
        //得到这个月
        Integer nowMonth=LocalDateTime.now().getMonthValue();
        Integer lastMonth=LocalDateTime.now().minusMonths(1).getMonthValue();

        LocalDateTime nowStart=LocalDateTime.of(nowYear,nowMonth,0,0,0,0);
        LocalDateTime nowEnd=LocalDateTime.now();
        LocalDateTime lastStart=LocalDateTime.of(lastYear,lastMonth,0,0,0,0);
        LocalDateTime lastEnd=lastStart.plusMonths(1);

        //学期id->课程-》详情-》任务-》记录
        List<Integer> evaTaskIdS=new ArrayList<>();
        if(semId==null){
            evaTaskIdS=getEvaTaskIdS(semId);
        }else {
            List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(null);
            evaTaskIdS=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
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
    public Optional<OneDayAddEvaDataCO> evaOneDayInfo(Integer day, Integer num, Integer semId) {
        SimpleDateFormat sf=new SimpleDateFormat("HH:00");
        DecimalFormat df=new DecimalFormat("#.#");

        LocalDateTime time =LocalDateTime.of(LocalDateTime.now().getYear(),LocalDateTime.now().getMonthValue(),LocalDateTime.now().getDayOfMonth(),0,0);
        List<TimeEvaNumCO> timeEvaNumCOS=new ArrayList<>();
        for(int i=0;i<num+1;i++){
            LocalDateTime start=time.minusDays(day).plusHours((24/num)*i);

            List<Integer> evaTaskIds=getEvaTaskIdS(semId);
            List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIds)
                    .between("create_time",start,start.plusHours(24/num)));
            TimeEvaNumCO timeEvaNumCO=new TimeEvaNumCO();
            LocalTime t=LocalTime.of(start.getHour(),start.getMinute());
            timeEvaNumCO.setTime(sf.format(t));
            timeEvaNumCO.setMoreEvaNum(formRecordDOS.size());
            timeEvaNumCOS.add(timeEvaNumCO);
        }
        OneDayAddEvaDataCO oneDayAddEvaDataCO=new OneDayAddEvaDataCO();
        oneDayAddEvaDataCO.setMoreNum(getEvaNumByDate(day,semId));
        oneDayAddEvaDataCO.setMorePercent(Double.parseDouble(df.format(((getEvaNumByDate(day,semId)/getEvaNumByDate(day+1,semId))*100))));
        oneDayAddEvaDataCO.setEvaNumArr(timeEvaNumCOS);
        return Optional.of(oneDayAddEvaDataCO);
    }

    //zjok
    @Override
    public Optional<PastTimeEvaDetailCO> getEvaData(Integer semId, Integer num, Integer target, Integer evaTarget) {
        DecimalFormat ds=new DecimalFormat("0.0");
        SimpleDateFormat sf=new SimpleDateFormat("YY-MM-DD");
        //根据semId找到
        List<Integer> evaTaskIdS=getEvaTaskIdS(semId);

        LocalDate timeEnd=LocalDate.now();
        LocalDate timeStart=LocalDate.now().minusDays((long)num);

        LocalDate lastStart=LocalDate.now().minusDays((long)2*num);
        LocalDate lastEnd=LocalDate.now().minusDays(num);
        if(evaTaskIdS==null){
            throw new QueryException("并没有找到相关任务");
        }
        List<FormRecordDO> formRecordDOS1=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).between("create_time",timeStart,timeEnd));
        List<FormRecordDO> formRecordDOS2=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).between("create_time",lastStart,lastEnd));
        SimpleEvaPercentCO totalEvaInfo=new SimpleEvaPercentCO();
        totalEvaInfo.setNum(formRecordDOS1.size());
        totalEvaInfo.setMorePercent(Double.parseDouble(ds.format((formRecordDOS1.size()/formRecordDOS2.size())*100)));

        List<TimeEvaNumCO> dataArr=new ArrayList<>();

        for(int i=1;i<=num;i++){
            List<FormRecordDO> formRecordDOS3=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).between("create_time",LocalDate.now().minusDays((long)num-i+1),LocalDate.now().minusDays((long)num-i)));
            TimeEvaNumCO timeEvaNumCO=new TimeEvaNumCO();
            timeEvaNumCO.setTime(sf.format(LocalDate.now().minusDays((long)num-i)));
            timeEvaNumCO.setMoreEvaNum(formRecordDOS3.size());
            dataArr.add(timeEvaNumCO);
        }
        //SimpleEvaPercentCO evaQualifiedInfo  SimpleEvaPercentCO qualifiedInfo
        List<SysUserDO> teacher=sysUserMapper.selectList(null);
        List<Integer> teacherIdS=teacher.stream().map(SysUserDO::getId).toList();

        Integer evaNum=0;
        Integer pastEvaNum=0;
        Integer evaEdNum=0;
        Integer pastEvaEdNum=0;
        for(int i=0;i<teacherIdS.size();i++){
            Integer n1=getEvaNumByTeacherIdAndLocalTime(teacherIdS.get(i),num,0);
            Integer n2=getEvaEdNumByTeacherIdAndLocalTime(teacherIdS.get(i),num,0);
            Integer m1=getEvaNumByTeacherIdAndLocalTime(teacherIdS.get(i),num*2,num);
            Integer m2=getEvaEdNumByTeacherIdAndLocalTime(teacherIdS.get(i),num*2,num);
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
        evaQualifiedInfo.setMorePercent(Double.parseDouble(ds.format((evaNum/pastEvaNum)*100)));

        SimpleEvaPercentCO qualifiedInfo=new SimpleEvaPercentCO();
        qualifiedInfo.setNum(evaEdNum);
        qualifiedInfo.setMorePercent(Double.parseDouble(ds.format((evaEdNum/pastEvaEdNum)*100)));

        PastTimeEvaDetailCO pastTimeEvaDetailCO=new PastTimeEvaDetailCO();
        pastTimeEvaDetailCO.setTotalEvaInfo(totalEvaInfo);
        pastTimeEvaDetailCO.setEvaQualifiedInfo(evaQualifiedInfo);
        pastTimeEvaDetailCO.setQualifiedInfo(qualifiedInfo);
        pastTimeEvaDetailCO.setDataArr(dataArr);

        return Optional.of(pastTimeEvaDetailCO);
    }
    //怎么得到query里面的keyword TODO
    @Override
    public Optional<UnqualifiedUserResultCO> getEvaTargetAmountUnqualifiedUser(Integer semId,UnqualifiedUserConditionalQuery query, Integer num, Integer target){
        //根据系查老师
        List<SysUserDO> teacher=new ArrayList<>();
        if(query.getDepartment()!=null) {
            teacher = sysUserMapper.selectList(new QueryWrapper<SysUserDO>().eq("department", query.getDepartment()));
        }else {
            teacher=sysUserMapper.selectList(null);
        }

        List<Integer> teacherIdS=teacher.stream().map(SysUserDO::getId).toList();

        if(teacherIdS==null){
            throw new QueryException("找不到相关的老师");
        }

        List<UnqualifiedUserInfoCO> dataArr=new ArrayList<>();
        //根据
        for(int i=0;i<teacherIdS.size();i++){
            Integer n=getEvaNumByTeacherId(teacherIdS.get(i),semId);
            if(n<target){
                UnqualifiedUserInfoCO unqualifiedUserInfoCO=new UnqualifiedUserInfoCO();
                unqualifiedUserInfoCO.setDepartment(sysUserMapper.selectById(teacherIdS.get(i)).getDepartment());
                unqualifiedUserInfoCO.setId(teacherIdS.get(i));
                unqualifiedUserInfoCO.setName(sysUserMapper.selectById(teacherIdS.get(i)).getName());
                unqualifiedUserInfoCO.setNum(n);
                dataArr.add(unqualifiedUserInfoCO);
            }
        }

        if(dataArr==null){
            throw new QueryException("数据故障，显示为0");
        }
        UnqualifiedUserResultCO unqualifiedUserResultCO=getUnqualifiedUserResultCO(dataArr,num);
        return Optional.of(unqualifiedUserResultCO);
    }
    @Override
    public Optional<UnqualifiedUserResultCO> getBeEvaTargetAmountUnqualifiedUser(Integer semId,UnqualifiedUserConditionalQuery query,Integer num,Integer target){
        //根据系查老师
        List<SysUserDO> teacher=new ArrayList<>();
        if(query.getDepartment()!=null) {
            teacher = sysUserMapper.selectList(new QueryWrapper<SysUserDO>().eq("department", query.getDepartment()));
        }else {
            teacher=sysUserMapper.selectList(null);
        }
        List<Integer> teacherIdS=teacher.stream().map(SysUserDO::getId).toList();

        if(teacherIdS==null){
            throw new QueryException("找不到相关的老师");
        }

        List<UnqualifiedUserInfoCO> dataArr=new ArrayList<>();

        //任务-》课程详情-》课程-》老师
        for(int i=0;i<teacherIdS.size();i++){
            Integer n=getEvaEdNumByTeacherId(teacherIdS.get(i),semId);
            if(n<target){
                UnqualifiedUserInfoCO unqualifiedUserInfoCO=new UnqualifiedUserInfoCO();
                unqualifiedUserInfoCO.setDepartment(sysUserMapper.selectById(teacherIdS.get(i)).getDepartment());
                unqualifiedUserInfoCO.setId(teacherIdS.get(i));
                unqualifiedUserInfoCO.setName(sysUserMapper.selectById(teacherIdS.get(i)).getName());
                unqualifiedUserInfoCO.setNum(n);
                dataArr.add(unqualifiedUserInfoCO);
            }
        }
        if(dataArr==null){
            throw new QueryException("数据故障，显示为0");
        }
        UnqualifiedUserResultCO unqualifiedUserResultCO=getUnqualifiedUserResultCO(dataArr,num);
        return Optional.of(unqualifiedUserResultCO);
    }
    @Override
    public PaginationResultEntity<UnqualifiedUserInfoCO> pageEvaUnqualifiedUserInfo(Integer semId,PagingQuery<UnqualifiedUserConditionalQuery> query, Integer target){
        List<Integer> userIds=new ArrayList<>();
        Page<SysUserDO> pageUser=new Page<>(query.getPage(),query.getSize());
        QueryWrapper<SysUserDO> queryWrapper = null;
        if(query.getQueryObj().getDepartment()!=null){
            queryWrapper.eq("department",query.getQueryObj().getDepartment());
        }
        if(query.getQueryObj().getKeyword()!=null){
            queryWrapper.eq("name",query.getQueryObj().getKeyword());
        }

        pageUser=sysUserMapper.selectPage(pageUser,queryWrapper);
        userIds=pageUser.getRecords().stream().map(SysUserDO::getId).toList();

        List<Integer> teacherIdS=new ArrayList<>();
        List<UnqualifiedUserInfoCO> records=new ArrayList<>();
        for(int i=0;i<userIds.size();i++){
            Integer k=getEvaNumByTeacherId(userIds.get(i),semId);
            if(k>=target){
                teacherIdS.add(userIds.get(i));
                UnqualifiedUserInfoCO unqualifiedUserInfoCO=new UnqualifiedUserInfoCO();
                unqualifiedUserInfoCO.setId(userIds.get(i));
                unqualifiedUserInfoCO.setNum(k);
                unqualifiedUserInfoCO.setDepartment(query.getQueryObj().getDepartment());
                unqualifiedUserInfoCO.setName(sysUserMapper.selectById(userIds.get(i)).getName());

                records.add(unqualifiedUserInfoCO);
            }
        }
        Page<UnqualifiedUserInfoCO> pageUnqualifiedUserInfoCO=new Page<>(query.getPage(), query.getSize(),records.size());

        return paginationConverter.toPaginationEntity(pageUnqualifiedUserInfoCO,records);
    }
    @Override
    public PaginationResultEntity<UnqualifiedUserInfoCO> pageBeEvaUnqualifiedUserInfo(Integer semId,PagingQuery<UnqualifiedUserConditionalQuery> query,Integer target){

        List<Integer> userIds=new ArrayList<>();
        Page<SysUserDO> pageUser=new Page<>(query.getPage(),query.getSize());
        QueryWrapper<SysUserDO> queryWrapper = null;
        if(query.getQueryObj().getDepartment()!=null){
            queryWrapper.eq("department",query.getQueryObj().getDepartment());
        }
        if(query.getQueryObj().getKeyword()!=null){
            queryWrapper.eq("name",query.getQueryObj().getKeyword());
        }

        pageUser=sysUserMapper.selectPage(pageUser,queryWrapper);
        userIds=pageUser.getRecords().stream().map(SysUserDO::getId).toList();

        List<Integer> teacherIdS=new ArrayList<>();
        List<UnqualifiedUserInfoCO> records=new ArrayList<>();
        for(int i=0;i<userIds.size();i++){
            Integer k=getEvaEdNumByTeacherId(userIds.get(i),semId);
            if(k>=target){
                teacherIdS.add(userIds.get(i));
                UnqualifiedUserInfoCO unqualifiedUserInfoCO=new UnqualifiedUserInfoCO();
                unqualifiedUserInfoCO.setId(userIds.get(i));
                unqualifiedUserInfoCO.setNum(k);
                unqualifiedUserInfoCO.setDepartment(query.getQueryObj().getDepartment());
                unqualifiedUserInfoCO.setName(sysUserMapper.selectById(userIds.get(i)).getName());

                records.add(unqualifiedUserInfoCO);
            }
        }
        Page<UnqualifiedUserInfoCO> pageUnqualifiedUserInfoCO=new Page<>(query.getPage(), query.getSize(),records.size());

        return paginationConverter.toPaginationEntity(pageUnqualifiedUserInfoCO,records);
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
        EvaTaskDO evaTaskDO=evaTaskMapper.selectOne(new QueryWrapper<EvaTaskDO>().in("id",evaTaskIdS).eq("id",taskId));
        CourInfDO courInfDO=courInfMapper.selectById(evaTaskDO.getCourInfId());
        //1.直接去快照那边拿到
        CourOneEvaTemplateDO courOneEvaTemplateDO=courOneEvaTemplateMapper.selectOne(new QueryWrapper<CourOneEvaTemplateDO>().eq("course_id",courInfDO.getCourseId()));
        //2.去课程那边拿到
        CourseDO courseDO=courseMapper.selectById(courInfDO.getCourseId());
        FormTemplateDO formTemplateDO=formTemplateMapper.selectOne(new QueryWrapper<FormTemplateDO>().eq("id",courseDO.getTemplateId()));
        //
        if(courOneEvaTemplateDO!=null&&formTemplateDO!=null){
            throw new QueryException("不是说快照和模板那个二选一嘛");
        }

        if(courOneEvaTemplateDO==null){
            return Optional.of(courOneEvaTemplateDO.getFormTemplate());
        }else {
            return Optional.of(formTemplateDO.getProps());
        }
    }

    @Override
    public List<ScoreRangeCourseCO> scoreRangeCourseInfo(Integer num, Integer interval) {
        //得到全部记录数据
        List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(null);
        List<String> strings=formRecordDOS.stream().map(FormRecordDO::getTextValue).toList();
        //整到每个记录的分
        List<Double> numbers =new ArrayList<>();
        for(int i=0;i<strings.size();i++){
            //整个方法把单个text整到平均分
            numbers.set(i, stringToSumAver(strings.get(i)));
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
        List<FormTemplateDO> formTemplateDOS=formTemplateMapper.selectList(null);
        List<EvaTemplateEntity> evaTemplateEntities=formTemplateDOS.stream().map(formTemplateDO->evaConvertor.ToEvaTemplateEntity(formTemplateDO)).toList();
        return evaTemplateEntities;
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
        if(evaTaskIds==null){
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
        if(courInfoIds==null){
            throw new QueryException("并没有找到相关课程详情");
        }
        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id",courInfoIds));
        List<Integer> evaTaskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
        if(evaTaskIds==null){
            throw new QueryException("并没有找到相关任务");
        }
        List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIds));
        return Optional.of(formRecordDOS.size());
    }

    @Override
    public Optional<String> getNameByTaskId(Integer taskId) {
        return Optional.of(sysUserMapper.selectById(evaTaskMapper.selectById(taskId).getTeacherId()).getName());
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
        JSONObject jsonObject;
        try {
            jsonObject = JSONUtil.parseObj(s);
        }catch (Exception e){
            throw new SysException("jsonObject 数据对象转化失败");
        }
        Iterator<Map.Entry<String, Object>> iter = jsonObject.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = iter.next();
            score=score+(Double) entry.getValue();
        }
        return score/jsonObject.size();
    }
    //根据传来的前n天,还有evaTaskIdS返回SimplePercent对象
    private SimplePercentCO getSimplePercent(Integer n,List<Integer> evaTaskIdS,Double score){
        SimpleDateFormat sf=new SimpleDateFormat("YY-MM-DD");
        DecimalFormat df = new DecimalFormat("0.0");
        List<FormRecordDO> lastFormRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).gt("create_time",LocalDateTime.now().minusDays(n)));
        //总评教数
        Integer totalNum=lastFormRecordDOS.size();
        //根据他们的form_props_values得到对应的数值
        List<String> strings=lastFormRecordDOS.stream().map(FormRecordDO::getTextValue).toList();
        List<Double> numbers =new ArrayList<>();
        //低于 指定分数的数目
        Integer higherNum=0;
        for(int i=0;i<strings.size();i++){
            //整个方法把单个text整到平均分
            numbers.set(i, stringToSumAver(strings.get(i)));
            if(score<numbers.get(i)){
                higherNum++;
            }
        }
        Double percent=Double.parseDouble(df.format((higherNum/(double)totalNum)*100.0));
        SimplePercentCO simplePercentCO=new SimplePercentCO();
        simplePercentCO.setValue(percent);
        simplePercentCO.setDate(sf.format(LocalDate.now().minusDays(n)));
        return simplePercentCO;
    }
    //根据传来的学期id返回evaTaskIdS
    private List<Integer> getEvaTaskIdS(Integer semId){
        if(semId==null){
            List<EvaTaskDO> evaTaskDOS = evaTaskMapper.selectList(null);
            List<Integer> evaTaskIdS = evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
            return evaTaskIdS;
        }else {
            List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("sem_id", semId));

            if (courseDOS == null) {
                throw new QueryException("并未找到相关课程");
            }
            List<Integer> courseIdS = courseDOS.stream().map(CourseDO::getId).toList();

            if(courseIdS==null){
                throw new QueryException("并未找到相关课程");
            }

            List<CourInfDO> courInfDOS = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id", courseIdS));
            List<Integer> courInfoIdS = courInfDOS.stream().map(CourInfDO::getId).toList();
            if(courInfoIdS==null){
                throw new QueryException("并未找到相关课程");
            }
            List<EvaTaskDO> evaTaskDOS = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id", courInfoIdS));

            if (evaTaskDOS == null) {
                throw new QueryException("并未找到相关评教任务");
            }
            List<Integer> evaTaskIdS = evaTaskDOS.stream().map(EvaTaskDO::getId).toList();

            return evaTaskIdS;
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
            throw new QueryException("未找到相关的任务");
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
        List<EvaTaskDO> evaTaskDOS;
        if(semId!=null){
            List<Integer> evaIds=getEvaTaskIdS(semId);
            evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id",teacherId).in("id",evaIds));
        }else {
            evaTaskDOS = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id", teacherId));
        }
        if(evaTaskDOS==null){
            throw new QueryException("并未找到老师id对应的老师");
        }
        List<Integer> taskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();

        List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds));

        if(formRecordDOS==null){
            throw new QueryException("并未找到评教记录");
        }
        List<Integer> recordIds=formRecordDOS.stream().map(FormRecordDO::getId).toList();

        return recordIds.size();
    }
    //
    private Integer getEvaEdNumByTeacherId(Integer teacherId,Integer semId){
        List<CourseDO> courseDOS;
        if(semId!=null) {
            courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", teacherId).eq("semester_id",semId));
        }else {
            courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", teacherId));
        }
        if(courseDOS==null){
            throw new QueryException("并未找到老师id对应的老师");
        }
        List<Integer> courIdS=courseDOS.stream().map(CourseDO::getId).toList();

        List<CourInfDO> courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id",courIdS));
        List<Integer> courInfoIdS=courInfDOS.stream().map(CourInfDO::getId).toList();

        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id",courInfoIdS));
        List<Integer> taskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();

        List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds));

        if(formRecordDOS==null){
            throw new QueryException("并未找到评教记录");
        }
        List<Integer> recordIds=formRecordDOS.stream().map(FormRecordDO::getId).toList();

        return recordIds.size();
    }
    private Integer getEvaNumByTeacherIdAndLocalTime(Integer teacherId,Integer num1,Integer num2){
        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id",teacherId));
        List<Integer> taskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();

        if(num1<num2){
            throw new SysException("你的输入数字num有问题");
        }

        List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds).between("create_time",LocalDate.now().minusDays(num1),LocalDate.now().minusDays(num2)));

        if(formRecordDOS==null){
            throw new QueryException("并未找到评教记录");
        }
        List<Integer> recordIds=formRecordDOS.stream().map(FormRecordDO::getId).toList();

        return recordIds.size();
    }
    private Integer getEvaEdNumByTeacherIdAndLocalTime(Integer teacherId,Integer num1,Integer num2){
        if(num1<num2){
            throw new SysException("你的输入数字num有问题");
        }

        List<CourseDO> courseDOS=courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id",teacherId));
        List<Integer> courIdS=courseDOS.stream().map(CourseDO::getId).toList();

        List<CourInfDO> courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id",courIdS));
        List<Integer> courInfoIdS=courInfDOS.stream().map(CourInfDO::getId).toList();

        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id",courInfoIdS));
        List<Integer> taskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();

        List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds).between("create_time",LocalDateTime.now().minusDays(num1),LocalDateTime.now().minusDays(num2)));

        if(formRecordDOS==null){
            throw new QueryException("并未找到评教记录");
        }
        List<Integer> recordIds=formRecordDOS.stream().map(FormRecordDO::getId).toList();

        return recordIds.size();
    }
    private UnqualifiedUserResultCO getUnqualifiedUserResultCO(List<UnqualifiedUserInfoCO> dataArr,Integer num){
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
            getDataArr.add(dataArr.get(i));
        }
        UnqualifiedUserResultCO unqualifiedUserResultCO=new UnqualifiedUserResultCO();
        unqualifiedUserResultCO.setDataArr(getDataArr);
        unqualifiedUserResultCO.setTotal(dataArr.size());

        return unqualifiedUserResultCO;
    }
    private List<SingleCourseEntity> getListCurInfoEntities(List<CourInfDO> courInfDOS,Integer semId){
        return courInfDOS.stream().map(courInfDO ->courseConvertor.toSingleCourseEntity(
                ()->toCourseEntity(courInfDO.getCourseId(),semId),courInfDO)).toList();
    }

}
