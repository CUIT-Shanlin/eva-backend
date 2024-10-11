package edu.cuit.infra.gateway.impl.eva;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.cuit.client.dto.clientobject.SimpleEvaPercentCO;
import edu.cuit.client.dto.clientobject.SimplePercentCO;
import edu.cuit.client.dto.clientobject.TimeEvaNumCO;
import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import edu.cuit.client.dto.clientobject.eva.EvaScoreInfoCO;
import edu.cuit.client.dto.clientobject.eva.PastTimeEvaDetailCO;
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
import edu.cuit.domain.entity.user.biz.MenuEntity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
import edu.cuit.infra.convertor.PaginationConverter;
import edu.cuit.infra.convertor.course.CourseConvertor;
import edu.cuit.infra.convertor.eva.EvaConvertor;
import edu.cuit.infra.convertor.user.UserConverter;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import edu.cuit.infra.dal.database.dataobject.user.*;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SemesterMapper;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import edu.cuit.infra.dal.database.mapper.user.*;
import edu.cuit.infra.util.QueryUtils;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;


import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Component
@RequiredArgsConstructor
public class EvaQueryGatewayImpl implements EvaQueryGateway {
    private final CourseMapper courseMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final EvaConvertor evaConvertor;
    private final PaginationConverter paginationConverter;
    private final UserConverter userConverter;
    private final CourseConvertor courseConvertor;
    private final SysUserMapper sysUserMapper;
    private final SemesterMapper semesterMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SubjectMapper subjectMapper;
    private final CourInfMapper courInfMapper;
    private final FormRecordMapper formRecordMapper;
    private final FormTemplateMapper formTemplateMapper;
    @Override
    public List<EvaRecordEntity> pageEvaRecord(Integer semId, PagingQuery<EvaLogConditionalQuery> evaLogQuery) {
        //先整老师
        List<Integer> userIds=null;
        if(sysUserMapper.selectList(new QueryWrapper<SysUserDO>().like("name",evaLogQuery.getQueryObj().getKeyword()))!=null) {
            Page<SysUserDO> pageUser=new Page<>(evaLogQuery.getPage(),evaLogQuery.getSize());
            pageUser=sysUserMapper.selectPage(pageUser,new QueryWrapper<SysUserDO>().like("name",evaLogQuery.getQueryObj().getKeyword()));
            userIds=pageUser.getRecords().stream().map(SysUserDO::getId).toList();
        }
        List<SysUserDO> teachers=sysUserMapper.selectList(new QueryWrapper<SysUserDO>().in("id",userIds));
        List<UserEntity> userEntities=teachers.stream().map(sysUserDO->toUserEntity(sysUserDO.getId())).toList();
        //再整课程
        List<Integer> courseIds;
        Page<CourseDO> pageCourse=new Page<>(evaLogQuery.getPage(),evaLogQuery.getSize());
        if(subjectMapper.selectList(new QueryWrapper<SubjectDO>().like("name",evaLogQuery.getQueryObj().getKeyword()))!=null){

            List<SubjectDO> subjectDOS=subjectMapper.selectList(new QueryWrapper<SubjectDO>().like("name",evaLogQuery.getQueryObj().getKeyword()));
            List<Integer> subjectIds=subjectDOS.stream().map(SubjectDO::getId).toList();

            pageCourse=courseMapper.selectPage(pageCourse,new QueryWrapper<CourseDO>().in("id",subjectIds).eq("semId",semId));
            courseIds=pageCourse.getRecords().stream().map(CourseDO::getId).toList();
        }else {
            pageCourse=courseMapper.selectPage(pageCourse,new QueryWrapper<CourseDO>().eq("semId",semId));
            courseIds=pageCourse.getRecords().stream().map(CourseDO::getId).toList();
        }

        Page<FormRecordDO> pageLog=new Page<>(evaLogQuery.getPage(),evaLogQuery.getSize());
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
                toCourseEntity(courInfDO.getCourseId(),semId),courInfDO)).toList();


        //根据eva任务来找到eva记录
        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(evaTaskWrapper);
        List<Integer> evaTaskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();

        List<EvaTaskEntity> evaTaskEntities=getEvaTaskEntitys(evaTaskDOS,userEntities,courseEntities);

        QueryWrapper<FormRecordDO> formRecordWrapper=new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIds);

        QueryUtils.fileTimeQuery(formRecordWrapper,evaLogQuery.getQueryObj());

        pageLog = formRecordMapper.selectPage(pageLog,formRecordWrapper);

        List<FormRecordDO> records = pageLog.getRecords();
        List<EvaRecordEntity> list = records.stream().map(formRecordDO->evaConvertor.ToEvaRecordEntity(formRecordDO,
                evaTaskEntities.stream().filter(evaTaskDO->evaTaskDO.getId()
                        .equals(formRecordDO.getTaskId())).findFirst().get())).toList();
        return list;
    }

    @Override
    public List<EvaTaskEntity> pageEvaUnfinishedTask(Integer semId, PagingQuery<EvaTaskConditionalQuery> taskQuery) {
        //先整老师
        List<Integer> userIds=null;
        if(sysUserMapper.selectList(new QueryWrapper<SysUserDO>().like("name",taskQuery.getQueryObj().getKeyword()))!=null) {
            Page<SysUserDO> pageUser=new Page<>(taskQuery.getPage(),taskQuery.getSize());
            pageUser=sysUserMapper.selectPage(pageUser,new QueryWrapper<SysUserDO>().like("name",taskQuery.getQueryObj().getKeyword()));
            userIds=pageUser.getRecords().stream().map(SysUserDO::getId).toList();
        }
        List<SysUserDO> teachers=sysUserMapper.selectList(new QueryWrapper<SysUserDO>().in("id",userIds));
        List<UserEntity> userEntities=teachers.stream().map(sysUserDO->toUserEntity(sysUserDO.getId())).toList();
        //再整课程
        List<Integer> courseIds=null;
        Page<CourseDO> pageCourse=new Page<>(taskQuery.getPage(),taskQuery.getSize());
        if(subjectMapper.selectList(new QueryWrapper<SubjectDO>().like("name",taskQuery.getQueryObj().getKeyword()))!=null){

            List<SubjectDO> subjectDOS=subjectMapper.selectList(new QueryWrapper<SubjectDO>().like("name",taskQuery.getQueryObj().getKeyword()));
            List<Integer> subjectIds=subjectDOS.stream().map(SubjectDO::getId).toList();

            pageCourse=courseMapper.selectPage(pageCourse,new QueryWrapper<CourseDO>().in("id",subjectIds).eq("semId",semId));
            courseIds=pageCourse.getRecords().stream().map(CourseDO::getId).toList();
        }else {
            pageCourse=courseMapper.selectPage(pageCourse,new QueryWrapper<CourseDO>().eq("semId",semId));
            courseIds=pageCourse.getRecords().stream().map(CourseDO::getId).toList();
        }

        Page<EvaTaskDO> pageTask=new Page<>(taskQuery.getPage(),taskQuery.getSize());
        QueryWrapper<EvaTaskDO> evaTaskWrapper=new QueryWrapper<>();

        QueryUtils.fileTimeQuery(evaTaskWrapper,taskQuery.getQueryObj());

        if(userIds!=null){
            evaTaskWrapper.in("teacher_id",userIds);
        }
        List<CourInfDO> courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id",courseIds));
        List<Integer> courseInfoIds=courInfDOS.stream().map(CourInfDO::getId).toList();
        if(courseIds!=null){
            evaTaskWrapper.in("cour_inf_id",courseInfoIds);
        }
        List<SingleCourseEntity> courseEntities=courInfDOS.stream().map(courInfDO -> courseConvertor.toSingleCourseEntity(
                toCourseEntity(courInfDO.getCourseId(),semId),courInfDO)).toList();
        //未完成的任务
        evaTaskWrapper.eq("status",0);

        pageTask=evaTaskMapper.selectPage(pageTask,evaTaskWrapper);
        List<EvaTaskDO> records=pageTask.getRecords();

        List<EvaTaskEntity> evaTaskEntities=getEvaTaskEntitys(records,userEntities,courseEntities);

        return evaTaskEntities;
    }

    @Override
    public List<EvaTemplateEntity> pageEvaTemplate(Integer semId, PagingQuery<GenericConditionalQuery> query) {

        Page<FormTemplateDO> page =new Page<>(query.getPage(),query.getSize());
        QueryWrapper<FormTemplateDO> queryWrapper = new QueryWrapper<>();
        if(query.getQueryObj().getKeyword()!=null){
            queryWrapper.like("name",query.getQueryObj().getKeyword());
        }
        QueryUtils.fileTimeQuery(queryWrapper,query.getQueryObj());
        Page<FormTemplateDO> formTemplateDOPage = formTemplateMapper.selectPage(page, queryWrapper);
        return formTemplateDOPage.getRecords().stream().map(pageEvaTemplateDO -> evaConvertor.ToEvaTemplateEntity(pageEvaTemplateDO)).toList();
    }
    //ok
    @Override
    public List<EvaTaskEntity> evaSelfTaskInfo(Integer id, String keyword){
        //根据关键字来查询老师
        QueryWrapper<SysUserDO> teacherWrapper =new QueryWrapper<>();
        teacherWrapper.like("name",keyword);
        //关键字查询课程名称subject->课程->课程详情
        QueryWrapper<SubjectDO> subjectWrapper =new QueryWrapper<>();
        subjectWrapper.like("name",keyword);
        List<Integer> subjectIds=subjectMapper.selectList(subjectWrapper).stream().map(SubjectDO::getId).toList();

        if(teacherWrapper!=null||subjectWrapper!=null){

            List<CourseDO> courseDOS;
            if(subjectWrapper!=null){
                //subject->课程->课程详情
                courseDOS=courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id",id).in("subject_id",subjectIds));
            }else{
                courseDOS=courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id",id));
            }
            //eva任务->课程详情表->课程表->学期id
            List<Integer> courseIds=courseDOS.stream().map(CourseDO::getId).toList();
            List<CourInfDO> courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id",courseIds));
            List<Integer> courInfIds=courInfDOS.stream().map(CourInfDO::getId).toList();
            List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id",courInfIds)
                    //顺便选出没有完成的
                    .eq("status",0));

            List<SingleCourseEntity> courseEntities=courInfDOS.stream().map(courInfDO -> courseConvertor.toSingleCourseEntity(
                    toCourseEntity(courInfDO.getCourseId(),id),courInfDO)).toList();
            List<SysUserDO> teachers;


            if(teacherWrapper!=null){
                teachers=sysUserMapper.selectList(teacherWrapper);
            }else{
                teachers=sysUserMapper.selectList(null);
            }
            List<UserEntity> userEntities=teachers.stream().map(sysUserDO->toUserEntity(sysUserDO.getId())).toList();

            List<EvaTaskEntity> list=getEvaTaskEntitys(evaTaskDOS,userEntities,courseEntities);

            return list;
        }
        return null;
    };

    @Override
    public List<EvaRecordEntity> getEvaLogInfo(Integer id,String keyword) {
        //根据关键字来查询相关的课程或者老师
        QueryWrapper<SysUserDO> teacherWrapper =new QueryWrapper<>();
        teacherWrapper.like("name",keyword);
        //关键字查询课程名称subject->课程->课程详情
        QueryWrapper<SubjectDO> subjectWrapper =new QueryWrapper<>();
        subjectWrapper.like("name",keyword);
        List<Integer> subjectIds=subjectMapper.selectList(subjectWrapper).stream().map(SubjectDO::getId).toList();

        if(teacherWrapper!=null||subjectWrapper!=null){
            //评教记录-》评教任务-》课程详情表->课程表->学期id
            List<CourseDO> courseDOS;
            if(subjectWrapper!=null){
                //subject->课程->课程详情
                courseDOS=courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id",id).in("subject_id",subjectIds));
            }else{
                courseDOS=courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id",id));
            }
            //eva任务->课程详情表->课程表->学期id
            List<Integer> courseIds=courseDOS.stream().map(CourseDO::getId).toList();
            List<CourInfDO> courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id",courseIds));
            List<Integer> courInfIds=courInfDOS.stream().map(CourInfDO::getId).toList();
            List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id",courInfIds));

            List<Integer> evaTaskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
            List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIds));

            List<SingleCourseEntity> courseEntities=courInfDOS.stream().map(courInfDO -> courseConvertor.toSingleCourseEntity(
                    toCourseEntity(courInfDO.getCourseId(),id),courInfDO)).toList();
            List<SysUserDO> teachers;


            if(teacherWrapper!=null){
                teachers=sysUserMapper.selectList(teacherWrapper);
            }else{
                teachers=sysUserMapper.selectList(null);
            }
            List<UserEntity> userEntities=teachers.stream().map(sysUserDO->toUserEntity(sysUserDO.getId())).toList();

            List<EvaTaskEntity> evaTaskEntityList=getEvaTaskEntitys(evaTaskDOS,userEntities,courseEntities);

            List<EvaRecordEntity> list=formRecordDOS.stream().map(formRecordDO -> evaConvertor.ToEvaRecordEntity(formRecordDO,
                    evaTaskEntityList.stream().filter(evaTaskDO->evaTaskDO.getId()
                            .equals(formRecordDO.getTaskId())).findFirst().get())).toList();
            return list;
        }
        return null;
    }
    //ok
    @Override
    public Optional<EvaTaskEntity> oneEvaTaskInfo(Integer id) {
        EvaTaskDO evaTaskDO=evaTaskMapper.selectOne(new QueryWrapper<EvaTaskDO>().eq("id",id));
        //老师
        UserEntity teacher=toUserEntity(evaTaskDO.getTeacherId());
        //课程信息
        CourInfDO courInfDO=courInfMapper.selectById(evaTaskDO.getCourInfId());
        CourseDO courseDO=courseMapper.selectById(courInfDO.getCourseId());
        CourseEntity course=toCourseEntity(courInfDO.getCourseId(),courseDO.getSemesterId());
        SingleCourseEntity oneCourse=courseConvertor.toSingleCourseEntity(course,courInfDO);

        EvaTaskEntity evaTaskEntity=evaConvertor.ToEvaTaskEntity(evaTaskDO,teacher,oneCourse);
        return Optional.of(evaTaskEntity);
    }

    @Override
    public Optional<EvaScoreInfoCO> evaScoreStatisticsInfo(Integer semId, Number score) {
        //学期id->找到课程-》找到课程详情-》评教任务详情-》评教表单记录里面
        List<Integer> evaTaskIdS=getEvaTaskIdS(semId);

        List<FormRecordDO> nowFormRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS));
        //总评教数
        Integer totalNum=nowFormRecordDOS.size();
        //根据他们的form_props_values得到对应的数值
        List<String> strings=nowFormRecordDOS.stream().map(FormRecordDO::getTextValue).toList();
        List<Double> numbers =null;
        //低于 指定分数的数目
        Double aScore=(Double) score;
        Integer lowerNum=0;
        Integer higherNum=0;
        for(int i=0;i<strings.size();i++){
            //整个方法把单个text整到平均分
            numbers.set(i, stringToSumAver(strings.get(i)));
            if(aScore>numbers.get(i)){
                lowerNum++;
            }
            if(aScore<numbers.get(i)){
                higherNum++;
            }
        }
        Double percent=(higherNum/totalNum)*100.0;
        //整个方法把以前的数据拿出来
        List<FormRecordDO> last1FormRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).gt("create_time",LocalDateTime.now().minusDays(1)));
        //总评教数
        Integer totalNum1=last1FormRecordDOS.size();
        //根据他们的form_props_values得到对应的数值
        List<String> strings1=last1FormRecordDOS.stream().map(FormRecordDO::getTextValue).toList();
        List<Double> numbers1 =null;
        //低于 指定分数的数目
        Integer lowerNum1=0;
        Integer higherNum1=0;
        for(int i=0;i<strings1.size();i++){
            //整个方法把单个text整到平均分
            numbers1.set(i, stringToSumAver(strings1.get(i)));
            if(aScore>numbers1.get(i)){
                lowerNum1++;
            }
            if(aScore<numbers1.get(i)){
                higherNum1++;
            }
        }
        Double percent1=(higherNum1/totalNum1)*100.0;
        //7日内 percent 的值
        List<SimplePercentCO> percentArr=null;
        for(int i=0;i<7;i++){
            percentArr.add(getSimplePercent(i,evaTaskIdS,aScore));
        }

        //构建EvaScoreInfoCO对象返回
        EvaScoreInfoCO evaScoreInfoCO=new EvaScoreInfoCO();
        evaScoreInfoCO.setLowerNum(lowerNum);
        evaScoreInfoCO.setTotalNum(totalNum);
        evaScoreInfoCO.setPercent(String.valueOf(percent));
        evaScoreInfoCO.setMoreNum(lowerNum-lowerNum1);
        evaScoreInfoCO.setMorePercent((int) (percent-percent1));
        evaScoreInfoCO.setPercentArr(percentArr);
        return Optional.of(evaScoreInfoCO);
    }
//ok
    @Override
    public List<Integer> getMonthEvaNUmber(Integer semId) {
        Integer nowMonth,lastMonth;
        Integer nowYear=LocalDateTime.now().getYear();
        Integer lastYear=LocalDateTime.now().minusMonths(1).getYear();
        //得到这个月
        nowMonth=LocalDateTime.now().getMonthValue();
        lastMonth=LocalDateTime.now().minusMonths(1).getMonthValue();

        LocalDateTime nowStart=LocalDateTime.of(nowYear,nowMonth,0,0,0,0);
        LocalDateTime nowEnd=LocalDateTime.of(nowYear,nowMonth,3,0,0,0);
        LocalDateTime lastStart=LocalDateTime.of(lastYear,lastMonth,0,0,0,0);
        LocalDateTime lastEnd=LocalDateTime.of(lastYear,lastMonth,0,0,0,0);


        //学期id->课程-》详情-》任务-》记录
        List<Integer> evaTaskIdS=getEvaTaskIdS(semId);

        List<FormRecordDO> nowFormRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).between("create_time",nowStart,nowEnd));
        List<FormRecordDO> lastFormRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).between("create_time",lastStart,lastEnd));
        //获取上个月和本月的评教数目，以有两个整数的List形式返回，data[0]：上个月评教数目；data[1]：本月评教数目
        List<Integer> list=new ArrayList<Integer>();
        list.add(lastFormRecordDOS.size());
        list.add(nowFormRecordDOS.size());
        return list;
    }

    @Override
    public Optional<PastTimeEvaDetailCO> getEvaData(Integer semId, Integer num, Integer target, Integer evaTarget) {
        //根据semId找到
        List<Integer> evaTaskIdS=getEvaTaskIdS(semId);
        List<FormRecordDO> formRecordDOS1=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).between("create_time",LocalDateTime.now().minusDays(num),LocalDateTime.now()));
        List<FormRecordDO> formRecordDOS2=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).between("create_time",LocalDateTime.now().minusDays(2*num),LocalDateTime.now().minusDays(num)));
        SimpleEvaPercentCO totalEvaInfo=new SimpleEvaPercentCO();
        totalEvaInfo.setNum(formRecordDOS1.size());
        totalEvaInfo.setMorePercent((formRecordDOS1.size()/formRecordDOS2.size())*100);

        List<TimeEvaNumCO> dataArr=null;

        for(int i=1;i<=num;i++){
            List<FormRecordDO> formRecordDOS3=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).between("create_time",LocalDateTime.now().minusDays(num-i+1),LocalDateTime.now().minusDays(num-i)));
            TimeEvaNumCO timeEvaNumCO=new TimeEvaNumCO();
            timeEvaNumCO.setTime(String.valueOf(LocalDateTime.now().minusDays(num-i)));
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
        evaQualifiedInfo.setMorePercent((evaNum/pastEvaNum)*100);

        SimpleEvaPercentCO qualifiedInfo=new SimpleEvaPercentCO();
        qualifiedInfo.setNum(evaEdNum);
        qualifiedInfo.setMorePercent((evaEdNum/pastEvaEdNum)*100);

        PastTimeEvaDetailCO pastTimeEvaDetailCO=new PastTimeEvaDetailCO();
        pastTimeEvaDetailCO.setTotalEvaInfo(totalEvaInfo);
        pastTimeEvaDetailCO.setEvaQualifiedInfo(evaQualifiedInfo);
        pastTimeEvaDetailCO.setQualifiedInfo(qualifiedInfo);
        pastTimeEvaDetailCO.setDataArr(dataArr);

        return Optional.of(pastTimeEvaDetailCO);
    }
    @Override
    public Optional<UnqualifiedUserResultCO> getEvaTargetAmountUnqualifiedUser(UnqualifiedUserConditionalQuery query, Integer num, Integer target){
        //根据系查老师
        List<SysUserDO> teacher=sysUserMapper.selectList(new QueryWrapper<SysUserDO>().eq("department",query.getDepartment()));
        List<Integer> teacherIdS=teacher.stream().map(SysUserDO::getId).toList();

        List<UnqualifiedUserInfoCO> dataArr=null;
        //根据
        for(int i=0;i<teacherIdS.size();i++){
            Integer n=getEvaNumByTeacherId(teacherIdS.get(i));
            if(n<target){
                UnqualifiedUserInfoCO unqualifiedUserInfoCO=new UnqualifiedUserInfoCO();
                unqualifiedUserInfoCO.setDepartment(query.getDepartment());
                unqualifiedUserInfoCO.setId(teacherIdS.get(i));
                unqualifiedUserInfoCO.setName(sysUserMapper.selectById(teacherIdS.get(i)).getName());
                unqualifiedUserInfoCO.setNum(n);
                dataArr.add(unqualifiedUserInfoCO);
            }
        }
        return Optional.of(getUnqualifiedUserResultCO(dataArr,num));
    }

    public Optional<UnqualifiedUserResultCO> getBeEvaTargetAmountUnqualifiedUser(UnqualifiedUserConditionalQuery query,Integer num,Integer target){
        //根据系查老师
        List<SysUserDO> teacher=sysUserMapper.selectList(new QueryWrapper<SysUserDO>().eq("department",query.getDepartment()));
        List<Integer> teacherIdS=teacher.stream().map(SysUserDO::getId).toList();

        List<UnqualifiedUserInfoCO> dataArr=null;

        //任务-》课程详情-》课程-》老师
        for(int i=0;i<teacherIdS.size();i++){
            Integer n=getEvaEdNumByTeacherId(teacherIdS.get(i));
            if(n<target){
                UnqualifiedUserInfoCO unqualifiedUserInfoCO=new UnqualifiedUserInfoCO();
                unqualifiedUserInfoCO.setDepartment(query.getDepartment());
                unqualifiedUserInfoCO.setId(teacherIdS.get(i));
                unqualifiedUserInfoCO.setName(sysUserMapper.selectById(teacherIdS.get(i)).getName());
                unqualifiedUserInfoCO.setNum(n);
                dataArr.add(unqualifiedUserInfoCO);
            }
        }
        return Optional.of(getUnqualifiedUserResultCO(dataArr, num));
    }

    public PaginationResultEntity<UnqualifiedUserInfoCO> pageEvaUnqualifiedUserInfo(PagingQuery<UnqualifiedUserConditionalQuery> query, Integer target){
        List<Integer> userIds=null;
        Page<SysUserDO> pageUser=new Page<>(query.getPage(),query.getSize());
        if(query.getQueryObj().getDepartment()!=null){
            pageUser=sysUserMapper.selectPage(pageUser,new QueryWrapper<SysUserDO>().like("department",query.getQueryObj().getDepartment()));
            userIds=pageUser.getRecords().stream().map(SysUserDO::getId).toList();
        }
        List<Integer> teacherIdS=null;
        List<UnqualifiedUserInfoCO> records=null;
        for(int i=0;i<userIds.size();i++){
            Integer k=getEvaNumByTeacherId(userIds.get(i));
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
        Page<UnqualifiedUserInfoCO> pageUnqualifiedUserInfoCO=new Page<UnqualifiedUserInfoCO>(query.getPage(), query.getSize(),records.size());

        PaginationResultEntity<UnqualifiedUserInfoCO> paginationEntity = paginationConverter.toPaginationEntity(pageUnqualifiedUserInfoCO,records);

        return paginationEntity;
    }

    public PaginationResultEntity<UnqualifiedUserInfoCO> pageBeEvaUnqualifiedUserInfo(PagingQuery<UnqualifiedUserConditionalQuery> query,Integer target){
        List<Integer> userIds=null;
        Page<SysUserDO> pageUser=new Page<>(query.getPage(),query.getSize());
        if(query.getQueryObj().getDepartment()!=null){
            pageUser=sysUserMapper.selectPage(pageUser,new QueryWrapper<SysUserDO>().like("department",query.getQueryObj().getDepartment()));
            userIds=pageUser.getRecords().stream().map(SysUserDO::getId).toList();
        }
        List<Integer> teacherIdS=null;
        List<UnqualifiedUserInfoCO> records=null;
        for(int i=0;i<userIds.size();i++){
            Integer k=getEvaEdNumByTeacherId(userIds.get(i));
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
        Page<UnqualifiedUserInfoCO> pageUnqualifiedUserInfoCO=new Page<UnqualifiedUserInfoCO>(query.getPage(), query.getSize(),records.size());

        PaginationResultEntity<UnqualifiedUserInfoCO> paginationEntity = paginationConverter.toPaginationEntity(pageUnqualifiedUserInfoCO,records);

        return paginationEntity;
    }

    @Override
    public Integer getEvaNumber(Long id) {
        //获取用户已评教数目用户id
        //用户id-》查询评教任务的老师-》查询status==1(已评教)
        QueryWrapper<EvaTaskDO> taskWrapper=new QueryWrapper<EvaTaskDO>().eq("teacher_id",id).eq("status",1);
        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(taskWrapper);
        Integer a=evaTaskDOS.size();
        return a;
    }



    //简便方法
    private UserEntity toUserEntity(Integer userId){
        //得到uer对象
        SysUserDO userDO = sysUserMapper.selectById(userId);
        //根据userId找到角色id集合
        List<Integer> roleIds = sysUserRoleMapper.selectList(new QueryWrapper<SysUserRoleDO>().eq("user_id", userId)).stream().map(SysUserRoleDO::getRoleId).toList();
        //根据角色id集合找到角色对象集合
        List<RoleEntity> roleEntities = sysRoleMapper.selectList(new QueryWrapper<SysRoleDO>().in("id", roleIds)).stream().map(roleDO -> userConverter.toRoleEntity(roleDO)).toList();
        //根据角色id集合找到角色菜单表中的菜单id集合
        List<Integer> menuIds = sysRoleMenuMapper.selectList(new QueryWrapper<SysRoleMenuDO>().in("role_id", roleIds)).stream().map(SysRoleMenuDO::getMenuId).toList();
        //根据menuids找到菜单对象集合
        List<MenuEntity> menuEntities = sysMenuMapper.selectList(new QueryWrapper<SysMenuDO>().in("id", menuIds)).stream().map(menuDO -> userConverter.toMenuEntity(menuDO)).toList();
        return userConverter.toUserEntity(userDO,roleEntities,menuEntities);
    }
    private CourseEntity toCourseEntity(Integer courseId,Integer semId){
        //构造semester
        SemesterEntity semesterEntity = courseConvertor.toSemesterEntity(semesterMapper.selectById(semId));
        //构造courseDo
        CourseDO courseDO = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", courseId).eq("semester_id", semId));
        //构造subject
        SubjectEntity subjectEntity = courseConvertor.toSubjectEntity(subjectMapper.selectById(courseDO.getSubjectId()));
        //构造userEntity
        UserEntity userEntity =toUserEntity(courseMapper.selectById(courseId).getTeacherId());
        return courseConvertor.toCourseEntity(courseDO,subjectEntity,userEntity,semesterEntity);
    }
    //根据传来的String数据form_props_values中的数据解析出来得到平均分
    private Double stringToSumAver(String s) {
        Double averScore= 0.0;
        Double score=0.0;
        JSONObject jsonObject = JSONUtil.parseObj(s);
        Iterator iter = jsonObject.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            score=score+(Double) entry.getValue();
        }
        averScore=score/jsonObject.size();
        return averScore;
    }
    //根据传来的前n天,还有evaTaskIdS返回SimplePercent对象
    private SimplePercentCO getSimplePercent(Integer n,List<Integer> evaTaskIdS,Double score){
        List<FormRecordDO> lastFormRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).gt("create_time",LocalDateTime.now().minusDays(n)));
        //总评教数
        Integer totalNum=lastFormRecordDOS.size();
        //根据他们的form_props_values得到对应的数值 TODO
        List<String> strings=lastFormRecordDOS.stream().map(FormRecordDO::getTextValue).toList();
        List<Double> numbers =null;
        //低于 指定分数的数目
        Integer lowerNum=0;
        Integer higherNum=0;
        for(int i=0;i<strings.size();i++){
            //整个方法把单个text整到平均分
            numbers.set(i, stringToSumAver(strings.get(i)));
            if(score<numbers.get(i)){
                higherNum++;
            }
        }
        Double percent=(higherNum/totalNum)*100.0;
        SimplePercentCO simplePercentCO=new SimplePercentCO();
        simplePercentCO.setValue(percent);
        simplePercentCO.setDate(String.valueOf(LocalDateTime.now().minusMonths(n)));
        return simplePercentCO;
    }
    //根据传来的学期id返回evaTaskIdS
    private List<Integer> getEvaTaskIdS(Integer semId){
        List<CourseDO> courseDOS=courseMapper.selectList(new QueryWrapper<CourseDO>().eq("sem_id",semId));
        List<Integer> courseIdS=courseDOS.stream().map(CourseDO::getId).toList();

        List<CourInfDO> courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id",courseIdS));
        List<Integer> courInfoIdS=courInfDOS.stream().map(CourInfDO::getId).toList();

        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id",courInfoIdS));
        List<Integer> evaTaskIdS=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();

        return evaTaskIdS;
    }

    //根据evaTaskDOs变成entity数据
    private List<EvaTaskEntity> getEvaTaskEntitys(List<EvaTaskDO> evaTaskDOS,List<UserEntity> userEntities,List<SingleCourseEntity> courseEntities){
        List<EvaTaskEntity> evaTaskEntityList=evaTaskDOS.stream().map(evaTaskDO -> evaConvertor.ToEvaTaskEntity(evaTaskDO,
                userEntities.stream().filter(sysUserDO->sysUserDO.getId()
                        .equals(evaTaskDO.getTeacherId())).findFirst().get(),
                courseEntities.stream().filter(courInfDO->courInfDO.getId()
                        .equals(evaTaskDO.getCourInfId())).findFirst().get())).toList();
        return evaTaskEntityList;
    }
    //
    private Integer getEvaNumByTeacherId(Integer teacherId){
        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id",teacherId));
        List<Integer> taskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();

        List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds));
        List<Integer> recordIds=formRecordDOS.stream().map(FormRecordDO::getId).toList();

        return recordIds.size();
    }
    //
    private Integer getEvaEdNumByTeacherId(Integer teacherId){
        List<CourseDO> courseDOS=courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id",teacherId));
        List<Integer> courIdS=courseDOS.stream().map(CourseDO::getId).toList();

        List<CourInfDO> courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id",courIdS));
        List<Integer> courInfoIdS=courInfDOS.stream().map(CourInfDO::getId).toList();

        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id",courInfoIdS));
        List<Integer> taskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();

        List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds));
        List<Integer> recordIds=formRecordDOS.stream().map(FormRecordDO::getId).toList();

        return recordIds.size();
    }
    private Integer getEvaNumByTeacherIdAndLocalTime(Integer teacherId,Integer num1,Integer num2){
        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id",teacherId));
        List<Integer> taskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();

        List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds).between("create_time",LocalDateTime.now().minusDays(num1),LocalDateTime.now().minusDays(num2)));
        List<Integer> recordIds=formRecordDOS.stream().map(FormRecordDO::getId).toList();

        return recordIds.size();
    }
    private Integer getEvaEdNumByTeacherIdAndLocalTime(Integer teacherId,Integer num1,Integer num2){
        List<CourseDO> courseDOS=courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id",teacherId));
        List<Integer> courIdS=courseDOS.stream().map(CourseDO::getId).toList();

        List<CourInfDO> courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id",courIdS));
        List<Integer> courInfoIdS=courInfDOS.stream().map(CourInfDO::getId).toList();

        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id",courInfoIdS));
        List<Integer> taskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();

        List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds).between("create_time",LocalDateTime.now().minusDays(num1),LocalDateTime.now().minusDays(num2)));
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
        List<UnqualifiedUserInfoCO> getDataArr=null;
        for(int i=0;i<num;i++){
            getDataArr.add(dataArr.get(i));
        }
        UnqualifiedUserResultCO unqualifiedUserResultCO=new UnqualifiedUserResultCO();
        unqualifiedUserResultCO.setDataArr(getDataArr);
        unqualifiedUserResultCO.setTotal(dataArr.size());

        return unqualifiedUserResultCO;
    }

}




