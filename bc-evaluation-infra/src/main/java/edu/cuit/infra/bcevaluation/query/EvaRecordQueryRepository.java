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
import edu.cuit.client.dto.clientobject.*;
import edu.cuit.client.dto.clientobject.eva.*;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaLogConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.course.CourseEntity;
import edu.cuit.domain.entity.course.SemesterEntity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.course.SubjectEntity;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
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
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserRoleDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SemesterMapper;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.user.SysRoleMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserRoleMapper;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    @Override
    public Optional<Double> getScoreFromRecord(String prop) {
        Double score =stringToSumAver(prop);
        return Optional.of(score);
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

    private List<SingleCourseEntity> getListCurInfoEntities(List<CourInfDO> courInfDOS){
        return courInfDOS.stream().map(courInfDO ->courseConvertor.toSingleCourseEntity(
                ()->toCourseEntity(courInfDO.getCourseId(),courseMapper.selectById(courInfDO.getCourseId()).getSemesterId()),courInfDO)).toList();
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
