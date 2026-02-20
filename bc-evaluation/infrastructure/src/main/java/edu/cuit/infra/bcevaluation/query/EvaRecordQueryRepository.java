package edu.cuit.infra.bcevaluation.query;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.cola.exception.SysException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.cuit.bc.course.application.port.CourseAndSemesterObjectDirectQueryPort;
import edu.cuit.bc.course.application.port.CourInfObjectDirectQueryPort;
import edu.cuit.bc.course.application.port.CourseEntityConvertPort;
import edu.cuit.bc.iam.application.port.UserEntityFieldExtractPort;
import edu.cuit.bc.iam.application.port.UserEntityObjectByIdDirectQueryPort;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaLogConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.course.CourseEntity;
import edu.cuit.domain.entity.course.SemesterEntity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.course.SubjectEntity;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
import edu.cuit.infra.convertor.PaginationConverter;
import edu.cuit.infra.convertor.eva.EvaConvertor;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 评教记录读侧 QueryRepo 实现（从 {@link EvaQueryRepository} 渐进式拆分出来）。
 *
 * <p>保持行为不变：仅搬运实现与依赖归属，不调整查询口径与异常文案。</p>
 */
@Primary
@Component
@RequiredArgsConstructor
public class EvaRecordQueryRepository implements EvaRecordQueryRepo {
    private final CourseAndSemesterObjectDirectQueryPort courseAndSemesterObjectDirectQueryPort;
    private final EvaTaskMapper evaTaskMapper;
    private final EvaConvertor evaConvertor;
    private final PaginationConverter paginationConverter;
    private final UserEntityFieldExtractPort userEntityFieldExtractPort;
    private final UserEntityObjectByIdDirectQueryPort userEntityObjectByIdDirectQueryPort;
    private final CourseEntityConvertPort courseEntityConvertPort;
    @Autowired
    @Qualifier("sysUserMapper")
    private Object sysUserMapper;
    private final SubjectMapper subjectMapper;
    private final CourInfObjectDirectQueryPort courInfObjectDirectQueryPort;
    private final FormRecordMapper formRecordMapper;

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
            List<CourInfDO> courInfDOS = courInfObjectDirectQueryPort.findByIds(evaTaskDOS.stream().map(EvaTaskDO::getCourInfId).toList()).stream()
                    .map(CourInfDO.class::cast)
                    .collect(Collectors.toList());
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
            List<CourInfDO> courInfDOS1 = courInfObjectDirectQueryPort.findByIds(courInfIds).stream()
                    .map(CourInfDO.class::cast)
                    .collect(Collectors.toList());
            List<Integer> courseIds1=courInfDOS1.stream().map(CourInfDO::getCourseId).toList();
            if(CollectionUtil.isEmpty(courseIds1)){
                List list=new ArrayList();
                return paginationConverter.toPaginationEntity(pageLog,list);
            }
            courseWrapper.in("id",courseIds1);
        }

        if(query.getQueryObj().getDepartmentName()!=null&&StringUtils.isNotBlank(query.getQueryObj().getDepartmentName())){
            List<Integer> sysUserIds=selectSysUserList(new QueryWrapper<SysUserDO>().eq(query.getQueryObj().getKeyword()!=null,"department",query.getQueryObj().getDepartmentName()))
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
        List<CourseDO> courseDOList=courseAndSemesterObjectDirectQueryPort.findCourseList(courseWrapper);

        List<Integer> courseIds=courseDOList.stream().map(CourseDO::getId).toList();
        if(CollectionUtil.isEmpty(courseIds)){
            List list=new ArrayList();
            return paginationConverter.toPaginationEntity(pageLog,list);
        }
        //任务

        QueryWrapper<EvaTaskDO> evaTaskWrapper=new QueryWrapper<EvaTaskDO>();
        List<CourInfDO> courInfDOS = courInfObjectDirectQueryPort.findByCourseIds(courseIds).stream()
                .map(CourInfDO.class::cast)
                .collect(Collectors.toList());
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
        List<SysUserDO> sysUserDOS=selectSysUserList(new QueryWrapper<SysUserDO>().in("id",userIds));
        if(CollectionUtil.isEmpty(sysUserDOS)){
            List list=new ArrayList();
            return paginationConverter.toPaginationEntity(pageLog,list);
        }
        List<Object> userEntities=sysUserDOS.stream().map(sysUserDO->toUserEntity(sysUserDO.getId())).toList();

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

    //zjok
    @Override
    public List<EvaRecordEntity> getEvaLogInfo(Integer evaUserId,Integer id,String keyword){
        List<CourseDO> courseDOS;
        QueryWrapper<CourseDO> query=new QueryWrapper<CourseDO>();

        if(keyword!=null&&StringUtils.isNotBlank(keyword)) {
            //根据关键字来查询相关的课程或者老师
            QueryWrapper<SysUserDO> teacherWrapper = new QueryWrapper<>();
            teacherWrapper.like("name", keyword);
            List<Integer> teacherIds = selectSysUserList(teacherWrapper).stream().map(SysUserDO::getId).toList();
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
        courseDOS=courseAndSemesterObjectDirectQueryPort.findCourseList(query);

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
            courInfDOS = courInfObjectDirectQueryPort.findByCourseIds(courseIds).stream()
                    .map(CourInfDO.class::cast)
                    .collect(Collectors.toList());
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
        List<SingleCourseEntity> courseEntities=courInfDOS.stream().map(courInfDO -> courseEntityConvertPort.toSingleCourseEntity(
                ()->toCourseEntity(courInfDO.getCourseId(),courseAndSemesterObjectDirectQueryPort.findCourseById(courInfDO.getCourseId()).getSemesterId()),courInfDO)).toList();

        SysUserDO teacher=selectSysUserById(evaUserId);
        List<SysUserDO> teachers=new ArrayList<>();
        teachers.add(teacher);

        List<Object> userEntities=teachers.stream().map(sysUserDO->toUserEntity(sysUserDO.getId())).toList();

        List<EvaTaskEntity> evaTaskEntityList=getEvaTaskEntities(evaTaskDOS,userEntities,courseEntities);

        return getRecordEntities(formRecordDOS,evaTaskEntityList);
    }

    //zjok
    @Override
    public List<EvaRecordEntity> getEvaEdLogInfo(Integer userId, Integer semId, Integer courseId) {
        //课程id ->课程->courInfo->evaTask->record
        List<CourInfDO> courInfDOs=new ArrayList<>();
        if(courseId!=null&&courseId>0){
            courInfDOs = courInfObjectDirectQueryPort.findByCourseIds(List.of(courseId)).stream()
                    .map(CourInfDO.class::cast)
                    .collect(Collectors.toList());
        }else{
            QueryWrapper<CourseDO> courseDOQueryWrapper=new QueryWrapper<CourseDO>();
            if(semId!=null){
                courseDOQueryWrapper.eq("semester_id",semId).eq("teacher_id",userId);
            }else{
                courseDOQueryWrapper.eq("teacher_id",userId);
            }
            List<CourseDO> courseDO=courseAndSemesterObjectDirectQueryPort.findCourseList(courseDOQueryWrapper);
            List<Integer> couIds=courseDO.stream().map(CourseDO::getId).toList();
            if(CollectionUtil.isEmpty(couIds)){
                List list=new ArrayList();
                return list;
            }else {
                courInfDOs = courInfObjectDirectQueryPort.findByCourseIds(couIds).stream()
                        .map(CourInfDO.class::cast)
                        .collect(Collectors.toList());
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
            sysUserDOS=selectSysUserList(new QueryWrapper<SysUserDO>().in("id",userIds));
        }
        List<Object> userEntities=sysUserDOS.stream().map(sysUserDO->toUserEntity(sysUserDO.getId())).toList();

        List<SingleCourseEntity> singleCourseEntities=courInfDOs.stream().map(courInfDO -> courseEntityConvertPort.toSingleCourseEntity(
                ()->toCourseEntity(courInfDO.getCourseId(),courseAndSemesterObjectDirectQueryPort.findCourseById(courInfDO.getCourseId()).getSemesterId()),courInfDO)).toList();
        List<EvaTaskEntity> evaTaskEntities=getEvaTaskEntities(evaTaskDOS,userEntities,singleCourseEntities);

        return getRecordEntities(formRecordDOS,evaTaskEntities);
    }

    @Override
    public Optional<Double> getScoreFromRecord(String prop) {
        Double score =stringToSumAver(prop);
        return Optional.of(score);
    }

    @Override
    public List<EvaRecordEntity> getRecordByCourse(Integer courseId) {
        List<Integer> courseIds=new ArrayList<>();
        courseIds.add(courseId);
        List<CourInfDO> courInfDOS = courInfObjectDirectQueryPort.findByCourseIds(courseIds).stream()
                .map(CourInfDO.class::cast)
                .collect(Collectors.toList());
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
        List<Object> userEntities=new ArrayList<>();
        userEntities.add(toUserEntity(courseAndSemesterObjectDirectQueryPort.findCourseById(courseId).getTeacherId()));
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
        CourseDO courseDO=courseAndSemesterObjectDirectQueryPort.findCourseById(courseId);
        List<CourInfDO> courInfDOS = courInfObjectDirectQueryPort.findByCourseIds(List.of(courseDO.getId())).stream()
                .map(CourInfDO.class::cast)
                .collect(Collectors.toList());
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

    //简便方法
    private Object toUserEntity(Integer userId){
        Object userEntity = userEntityObjectByIdDirectQueryPort.findById(userId);
        if (userEntity == null) {
            throw new QueryException("并未找到相关用户");
        }
        return userEntity;
    }

    private CourseEntity toCourseEntity(Integer courseId,Integer semId){
        //构造semester
        Supplier<SemesterEntity> semesterEntity = () -> courseEntityConvertPort.toSemesterEntity(courseAndSemesterObjectDirectQueryPort.findSemesterById(semId));
        //构造courseDo
        CourseDO courseDO = courseAndSemesterObjectDirectQueryPort.findOneCourse(new QueryWrapper<CourseDO>().eq("id", courseId).eq("semester_id", semId));
        if(courseDO==null){
            throw new QueryException("并未找到相关课程");
        }
        //构造subject
        Supplier<SubjectEntity> subjectEntity = () -> courseEntityConvertPort.toSubjectEntity(subjectMapper.selectById(courseDO.getSubjectId()));
        //构造userEntity
        Supplier<?> userEntity =()->toUserEntity(courseAndSemesterObjectDirectQueryPort.findCourseById(courseId).getTeacherId());
        return courseEntityConvertPort.toCourseEntityWithTeacherObject(courseDO,subjectEntity,userEntity,semesterEntity);
    }

    //根据evaTaskDOs变成entity数据
    private List<EvaTaskEntity> getEvaTaskEntities(List<EvaTaskDO> evaTaskDOS,List<Object> userEntities,List<SingleCourseEntity> courseEntities){
        List<EvaTaskEntity> evaTaskEntityList=evaTaskDOS.stream().map(evaTaskDO ->evaConvertor.toEvaTaskEntityWithTeacherObject(evaTaskDO,
                ()->userEntities.stream().filter(sysUserDO->userEntityFieldExtractPort.userIdOf(sysUserDO)
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

    private List<SingleCourseEntity> getListCurInfoEntities(List<CourInfDO> courInfDOS){
        return courInfDOS.stream().map(courInfDO ->courseEntityConvertPort.toSingleCourseEntity(
                ()->toCourseEntity(courInfDO.getCourseId(),courseAndSemesterObjectDirectQueryPort.findCourseById(courInfDO.getCourseId()).getSemesterId()),courInfDO)).toList();
    }

    private List<SysUserDO> selectSysUserList(Object wrapper) {
        try {
            Method selectList = Arrays.stream(sysUserMapper.getClass().getMethods())
                    .filter(m -> m.getName().equals("selectList") && m.getParameterCount() == 1)
                    .findFirst()
                    .orElseThrow(() -> new NoSuchMethodException("selectList"));
            Object result = selectList.invoke(sysUserMapper, wrapper);
            return (List<SysUserDO>) result;
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (targetException instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(targetException);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private SysUserDO selectSysUserById(Serializable userId) {
        try {
            Method selectById = sysUserMapper.getClass().getMethod("selectById", Serializable.class);
            return (SysUserDO) selectById.invoke(sysUserMapper, userId);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (targetException instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(targetException);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
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
}
