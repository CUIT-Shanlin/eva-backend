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
    private final EvaStatisticsQueryRepository evaStatisticsQueryRepository;
    private final EvaRecordQueryRepository evaRecordQueryRepository;
    private final EvaTaskQueryRepository evaTaskQueryRepository;

    @Override
    public PaginationResultEntity<EvaRecordEntity> pageEvaRecord(Integer semId, PagingQuery<EvaLogConditionalQuery> query) {
        return evaRecordQueryRepository.pageEvaRecord(semId, query);
    }

    @Override
    public PaginationResultEntity<EvaTaskEntity> pageEvaUnfinishedTask(Integer semId, PagingQuery<EvaTaskConditionalQuery> taskQuery) {
        return evaTaskQueryRepository.pageEvaUnfinishedTask(semId, taskQuery);
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
        return evaTaskQueryRepository.evaSelfTaskInfo(userId, id, keyword);
    }
    //zjok
    @Override
    public List<EvaRecordEntity> getEvaLogInfo(Integer evaUserId,Integer id,String keyword){
        return evaRecordQueryRepository.getEvaLogInfo(evaUserId, id, keyword);
    }
    //zjok
    @Override
    public List<EvaRecordEntity> getEvaEdLogInfo(Integer userId, Integer semId, Integer courseId) {
        return evaRecordQueryRepository.getEvaEdLogInfo(userId, semId, courseId);
    }

    //zjok
    @Override
    public Optional<EvaTaskEntity> oneEvaTaskInfo(Integer id) {
        return evaTaskQueryRepository.oneEvaTaskInfo(id);
    }
//zjok
    @Override
    public Optional<EvaScoreInfoCO> evaScoreStatisticsInfo(Integer semId, Number score) {
        return evaStatisticsQueryRepository.evaScoreStatisticsInfo(semId, score);
    }
    //zjok
    @Override
    public Optional<EvaSituationCO> evaTemplateSituation(Integer semId) {
        return evaStatisticsQueryRepository.evaTemplateSituation(semId);
    }

    //zjok
    @Override
    public List<Integer> getMonthEvaNUmber(Integer semId) {
        return evaStatisticsQueryRepository.getMonthEvaNUmber(semId);
    }
//zjok
    @Override
    public Optional<EvaWeekAddCO> evaWeekAdd(Integer week,Integer semId) {
        return evaStatisticsQueryRepository.evaWeekAdd(week, semId);
    }

    //zjok
    @Override
    public Optional<PastTimeEvaDetailCO> getEvaData(Integer semId, Integer num, Integer target, Integer evaTarget) {
        return evaStatisticsQueryRepository.getEvaData(semId, num, target, evaTarget);
    }
    @Override
    public Optional<UnqualifiedUserResultCO> getEvaTargetAmountUnqualifiedUser(Integer semId, Integer num, Integer target){
        return evaStatisticsQueryRepository.getEvaTargetAmountUnqualifiedUser(semId, num, target);
    }
    @Override
    public Optional<UnqualifiedUserResultCO> getBeEvaTargetAmountUnqualifiedUser(Integer semId,Integer num,Integer target){
        return evaStatisticsQueryRepository.getBeEvaTargetAmountUnqualifiedUser(semId, num, target);
    }
    @Override
    public PaginationResultEntity<UnqualifiedUserInfoCO> pageEvaUnqualifiedUserInfo(Integer semId,PagingQuery<UnqualifiedUserConditionalQuery> query, Integer target){
        return evaStatisticsQueryRepository.pageEvaUnqualifiedUserInfo(semId, query, target);
    }
    @Override
    public PaginationResultEntity<UnqualifiedUserInfoCO> pageBeEvaUnqualifiedUserInfo(Integer semId,PagingQuery<UnqualifiedUserConditionalQuery> query,Integer target){
        return evaStatisticsQueryRepository.pageBeEvaUnqualifiedUserInfo(semId, query, target);
    }

    @Override
    public Optional<Integer> getEvaNumber(Long id) {
        return evaTaskQueryRepository.getEvaNumber(id);
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
        return evaStatisticsQueryRepository.scoreRangeCourseInfo(num, interval);
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
        return evaRecordQueryRepository.getScoreFromRecord(prop);
    }

    @Override
    public Optional<Integer> getEvaNumByCourInfo(Integer courInfId) {
        return evaRecordQueryRepository.getEvaNumByCourInfo(courInfId);
    }

    @Override
    public Optional<Integer> getEvaNumByCourse(Integer courseId) {
        return evaRecordQueryRepository.getEvaNumByCourse(courseId);
    }

    @Override
    public Optional<String> getNameByTaskId(Integer taskId) {
        return evaTaskQueryRepository.getNameByTaskId(taskId);
    }

    @Override
    public List<EvaRecordEntity> getRecordByCourse(Integer courseId) {
        return evaRecordQueryRepository.getRecordByCourse(courseId);
    }

    @Override
    public Optional<Double> getScoreByProp(String prop) {
        return evaRecordQueryRepository.getScoreByProp(prop);
    }

    @Override
    public List<Double> getScoresByProp(String props) {
        return evaRecordQueryRepository.getScoresByProp(props);
    }

    @Override
    public Map<String, Double> getScorePropMapByProp(String props) {
        return evaRecordQueryRepository.getScorePropMapByProp(props);
    }

    @Override
    public List<Integer> getCountAbEva(Integer semId, Integer userId) {
        return evaStatisticsQueryRepository.getCountAbEva(semId, userId);
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
