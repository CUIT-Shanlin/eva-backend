package edu.cuit.infra.bcevaluation.query;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.cuit.bc.course.application.port.CourseAndSemesterObjectDirectQueryPort;
import edu.cuit.bc.course.application.port.CourInfObjectDirectQueryPort;
import edu.cuit.bc.iam.application.port.UserEntityFieldExtractPort;
import edu.cuit.bc.iam.application.port.UserEntityObjectByIdDirectQueryPort;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaTaskConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.course.CourseEntity;
import edu.cuit.domain.entity.course.SemesterEntity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.course.SubjectEntity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
import edu.cuit.bc.course.application.port.CourseEntityConvertPort;
import edu.cuit.infra.convertor.PaginationConverter;
import edu.cuit.infra.convertor.eva.EvaConvertor;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.infra.util.QueryUtils;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.function.Supplier;

/**
 * 评教任务读侧 QueryRepo 实现（从 {@link EvaQueryRepository} 渐进式拆分出来）。
 *
 * <p>保持行为不变：仅搬运实现与依赖归属，不调整查询口径与异常文案。</p>
 */
@Primary
@Component
@RequiredArgsConstructor
public class EvaTaskQueryRepository implements EvaTaskQueryRepo {
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
    private final EvaCacheConstants evaCacheConstants;
    private final LocalCacheManager localCacheManager;

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

        List<CourseDO> courseDOList=courseAndSemesterObjectDirectQueryPort.findCourseList(courseWrapper);
        courseIds=courseDOList.stream().map(CourseDO::getId).toList();
        if(CollectionUtil.isEmpty(courseIds)){
            List list=new ArrayList();
            return paginationConverter.toPaginationEntity(pageTask,list);
        }

        QueryWrapper<EvaTaskDO> evaTaskWrapper=new QueryWrapper<EvaTaskDO>();

        List<CourInfDO> courInfDOS = courInfObjectDirectQueryPort.findByCourseIds(courseIds).stream()
                .map(CourInfDO.class::cast)
                .collect(Collectors.toList());
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

        List<?> sysUserDOS = selectSysUserList(null);
        if(CollectionUtil.isEmpty(sysUserDOS)){
            List list=new ArrayList();
            return paginationConverter.toPaginationEntity(pageTask,list);
        }
        List<Object> userEntities = sysUserDOS.stream()
                .map(sysUserDO -> toUserEntity(selectSysUserId(sysUserDO)))
                .toList();

        List<EvaTaskEntity> evaTaskEntities=getEvaTaskEntities(pageTask.getRecords(),userEntities,courseEntities);

        return paginationConverter.toPaginationEntity(pageTask,evaTaskEntities);
    }

    //zjok
    @Override
    public List<EvaTaskEntity> evaSelfTaskInfo(Integer userId,Integer id, String keyword){
        List<CourseDO> courseDOS;
        QueryWrapper<CourseDO> query=new QueryWrapper<CourseDO>();
        if(keyword!=null&&StringUtils.isNotBlank(keyword)) {
            //根据关键字来查询老师
            QueryWrapper teacherWrapper = new QueryWrapper();
            teacherWrapper.like("name", keyword);
            List<Integer> teacherIds = selectSysUserList(teacherWrapper).stream()
                    .map(this::selectSysUserId)
                    .toList();
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
        Object teacher = selectSysUserById(userId);
        List<Object> teachers=new ArrayList<>();
        teachers.add(teacher);

        List<Object> userEntities = teachers.stream()
                .map(sysUserDO -> toUserEntity(selectSysUserId(sysUserDO)))
                .toList();

        return getEvaTaskEntities(evaTaskDOS,userEntities,courseEntities);
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
            Supplier<?> teacher = () -> toUserEntity(evaTaskDO.getTeacherId());
            //课程信息
            CourInfDO courInfDO = (CourInfDO) courInfObjectDirectQueryPort.findById(evaTaskDO.getCourInfId());
            if (courInfDO == null) {
                throw new QueryException("并没有找到相关课程信息");
            }
            CourseDO courseDO = courseAndSemesterObjectDirectQueryPort.findCourseById(courInfDO.getCourseId());
            Supplier<CourseEntity> course = () -> toCourseEntity(courInfDO.getCourseId(), courseDO.getSemesterId());
            Supplier<SingleCourseEntity> oneCourse = () -> courseEntityConvertPort.toSingleCourseEntity(course, courInfDO);

            EvaTaskEntity evaTaskEntity = evaConvertor.toEvaTaskEntityWithTeacherObject(evaTaskDO, teacher, oneCourse);
            return Optional.of(evaTaskEntity);
        }else {
            //老师
            EvaTaskDO finalGetCached = getCached;
            Supplier<?> teacher = () -> toUserEntity(finalGetCached.getTeacherId());
            //课程信息
            CourInfDO courInfDO = (CourInfDO) courInfObjectDirectQueryPort.findById(getCached.getCourInfId());
            if (courInfDO == null) {
                throw new QueryException("并没有找到相关课程信息");
            }
            CourseDO courseDO = courseAndSemesterObjectDirectQueryPort.findCourseById(courInfDO.getCourseId());
            Supplier<CourseEntity> course = () -> toCourseEntity(courInfDO.getCourseId(), courseDO.getSemesterId());
            Supplier<SingleCourseEntity> oneCourse = () -> courseEntityConvertPort.toSingleCourseEntity(course, courInfDO);

            EvaTaskEntity evaTaskEntity = evaConvertor.toEvaTaskEntityWithTeacherObject(getCached, teacher, oneCourse);
            return Optional.of(evaTaskEntity);
        }
    }

    @Override
    public Optional<Integer> getEvaNumber(Long id) {
        //获取用户已评教数目用户id
        //用户id-》查询评教任务的老师-》查询status==1(已评教)
        QueryWrapper<EvaTaskDO> taskWrapper=new QueryWrapper<EvaTaskDO>().eq("teacher_id",id).eq("status",1);
        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(taskWrapper);
        return Optional.of(evaTaskDOS.size());
    }

    @Override
    public Optional<String> getNameByTaskId(Integer taskId) {
        return Optional.of(selectSysUserNameById(evaTaskMapper.selectById(taskId).getTeacherId()));
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
        return courseEntityConvertPort.toCourseEntityWithTeacherObject(courseDO, subjectEntity, userEntity, semesterEntity);
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

    private List<SingleCourseEntity> getListCurInfoEntities(List<CourInfDO> courInfDOS){
        return courInfDOS.stream().map(courInfDO -> courseEntityConvertPort.toSingleCourseEntity(
                ()->toCourseEntity(courInfDO.getCourseId(),courseAndSemesterObjectDirectQueryPort.findCourseById(courInfDO.getCourseId()).getSemesterId()),courInfDO)).toList();
    }

    private List<?> selectSysUserList(Object wrapper) {
        try {
            Method selectList = Arrays.stream(sysUserMapper.getClass().getMethods())
                    .filter(m -> m.getName().equals("selectList") && m.getParameterCount() == 1)
                    .findFirst()
                    .orElseThrow(() -> new NoSuchMethodException("selectList"));
            Object result = selectList.invoke(sysUserMapper, wrapper);
            return (List<?>) result;
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

    private Object selectSysUserById(Serializable userId) {
        try {
            Method selectById = sysUserMapper.getClass().getMethod("selectById", Serializable.class);
            return selectById.invoke(sysUserMapper, userId);
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

    private Integer selectSysUserId(Object sysUser) {
        try {
            Method getId = sysUser.getClass().getMethod("getId");
            return (Integer) getId.invoke(sysUser);
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

    private String selectSysUserNameById(Serializable userId) {
        Object sysUser = selectSysUserById(userId);
        try {
            Method getName = sysUser.getClass().getMethod("getName");
            return (String) getName.invoke(sysUser);
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
}
