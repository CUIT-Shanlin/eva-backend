package edu.cuit.infra.bcevaluation.query;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaTaskConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.course.CourseEntity;
import edu.cuit.domain.entity.course.SemesterEntity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.course.SubjectEntity;
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
import edu.cuit.infra.dal.database.dataobject.user.SysRoleDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserRoleDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SemesterMapper;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.user.SysRoleMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserRoleMapper;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.infra.util.QueryUtils;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    private List<SingleCourseEntity> getListCurInfoEntities(List<CourInfDO> courInfDOS){
        return courInfDOS.stream().map(courInfDO ->courseConvertor.toSingleCourseEntity(
                ()->toCourseEntity(courInfDO.getCourseId(),courseMapper.selectById(courInfDO.getCourseId()).getSemesterId()),courInfDO)).toList();
    }
}
