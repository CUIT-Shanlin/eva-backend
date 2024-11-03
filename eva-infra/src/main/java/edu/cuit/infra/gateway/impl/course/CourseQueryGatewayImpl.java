package edu.cuit.infra.gateway.impl.course;


import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import edu.cuit.client.bo.EvaProp;

import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.course.RecommendCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SingleCourseCO;
import edu.cuit.client.dto.clientobject.eva.CourseScoreCO;
import edu.cuit.client.dto.clientobject.eva.EvaTeacherInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.client.dto.data.course.CoursePeriod;
import edu.cuit.client.dto.data.course.CourseTime;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.client.dto.query.CourseQuery;
import edu.cuit.client.dto.query.PagingQuery;

import edu.cuit.client.dto.query.condition.CourseConditionalQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.client.dto.query.condition.MobileCourseQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.course.*;

import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.infra.convertor.PaginationConverter;
import edu.cuit.infra.convertor.course.CourseConvertor;
import edu.cuit.infra.convertor.user.MenuConvertor;
import edu.cuit.infra.convertor.user.RoleConverter;
import edu.cuit.infra.convertor.user.UserConverter;
import edu.cuit.infra.dal.database.dataobject.course.*;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import edu.cuit.infra.dal.database.dataobject.user.*;
import edu.cuit.infra.dal.database.mapper.course.*;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import edu.cuit.infra.dal.database.mapper.user.*;
import edu.cuit.infra.enums.cache.CourseCacheConstants;
import edu.cuit.infra.gateway.impl.course.operate.CourseRecommendExce;
import edu.cuit.infra.util.QueryUtils;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.cache.aspect.annotation.local.LocalCached;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;



import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseQueryGatewayImpl implements CourseQueryGateway {
    private final CourseConvertor courseConvertor;
    private final RoleConverter roleConverter;
    private final UserConverter userConverter;
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final CourseTypeCourseMapper courseTypeCourseMapper;
    private final CourseTypeMapper courseTypeMapper;
    private final SemesterMapper semesterMapper;
    private final SubjectMapper subjectMapper;
    private final SysUserMapper userMapper;
    private final CourOneEvaTemplateMapper CourOneEvaTemplateMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final FormRecordMapper formRecordMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final CourseRecommendExce courseRecommendExce;
    private final PaginationConverter paginationConverter;
    private final ObjectMapper objectMapper;
    private final FormTemplateMapper formTemplateMapper;
    private final LocalCacheManager cacheManager;
    private final CourseCacheConstants courseCacheConstants;

    @Override
    public PaginationResultEntity<CourseEntity> page(PagingQuery<CourseConditionalQuery> courseQuery, Integer semId) {
        if(courseQuery==null){
            PaginationResultEntity<CourseEntity> getCache = cacheManager.getCache(courseCacheConstants.COURSE_LIST_BY_SEM+semId);
            if(getCache==null){
                //获取所有的课程的基础信息
                PaginationResultEntity<CourseEntity> courseList = getCourseList(semId);
                //将courseList放入缓存中
                cacheManager.putCache(courseCacheConstants.COURSE_LIST_BY_SEM+semId,courseList);
               return courseList;
            }else {
                return getCache;
            }
        }
        Page<CourseDO> pageCourse=new Page<>(courseQuery.getPage(),courseQuery.getSize());
        // 根据courseQuery中的departmentName以及courseQuery中的页数和一页显示数到userMapper中找对应数量的对应用户id，
        List<Integer> userIds=new ArrayList<>();
        if(courseQuery.getQueryObj().getDepartmentName()!=null&&!courseQuery.getQueryObj().getDepartmentName().isEmpty()){
            List<SysUserDO> users = userMapper.selectList(new QueryWrapper<SysUserDO>().like("department", courseQuery.getQueryObj().getDepartmentName()));
            if (users.isEmpty())return paginationConverter.toPaginationEntity(pageCourse, new ArrayList<>());

            userIds=users.stream().map(SysUserDO::getId).toList();
        }
        //根据courseQuery中的create时间范围，和update时间范围查询semester_id为semId的CourseDO

        QueryWrapper<CourseDO> courseWrapper=new QueryWrapper<>();
        QueryUtils.fileTimeQuery(courseWrapper,courseQuery.getQueryObj());
        if(semId!=null){
            courseWrapper.eq("semester_id",semId);
        }
        if(!userIds.isEmpty()){
            courseWrapper.in(true,"teacher_id",userIds);
        }
        //关键字查询
        List<Integer> listSubject=new ArrayList<>();
        if(courseQuery.getQueryObj().getKeyword()!=null&& !courseQuery.getQueryObj().getKeyword().isEmpty()){
            List<SubjectDO> subjectDOS = subjectMapper.selectList(new QueryWrapper<SubjectDO>().like("name", courseQuery.getQueryObj().getKeyword()));
            if(subjectDOS.isEmpty())return paginationConverter.toPaginationEntity(pageCourse, new ArrayList<>());
            listSubject.addAll(subjectDOS.stream().map(SubjectDO::getId).toList());
        }
        courseWrapper.in(!listSubject.isEmpty(),"subject_id",listSubject);
        pageCourse = courseMapper.selectPage(pageCourse,courseWrapper);
        //将paginationEntity中的records类型转化成CourseEntity
        List<CourseDO> records = pageCourse.getRecords();
        List<CourseEntity> list = records.stream().map(courseDO -> toCourseEntity(courseDO.getId(), semId)).toList();
        return paginationConverter.toPaginationEntity(pageCourse, list);
    }

    @Override
    public Optional<CourseDetailCO> getCourseInfo(Integer id, Integer semId) {
        //根据id和semId来查询课程信息
        CourseDO courseDO = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", id).eq("semester_id", semId));
        if(courseDO==null){
            throw new QueryException("该课程不存在");
        }
        //根据id和semId来查询评教快照信息
        CourOneEvaTemplateDO courOneEvaTemplateDO = CourOneEvaTemplateMapper.selectOne(new QueryWrapper<CourOneEvaTemplateDO>().eq("course_id", id).eq("semester_id", semId));
        //将courOneEvaTemplateDO中的formtemplate(json)字符串，转换为EvaTemplateCO
        EvaTemplateCO evaTemplateCO=null;
        if(courOneEvaTemplateDO!=null&&courOneEvaTemplateDO.getFormTemplate()!=null){
            try {
                evaTemplateCO = objectMapper.readValue(courOneEvaTemplateDO.getFormTemplate(), EvaTemplateCO.class);
            } catch (JsonProcessingException e) {
                throw new QueryException("formTemplate暂时为空");
            }
        }
        else{
            evaTemplateCO=new EvaTemplateCO();
            FormTemplateDO form = formTemplateMapper.selectOne(new QueryWrapper<FormTemplateDO>().eq("id", 1));
            evaTemplateCO.setId(form.getId());
            evaTemplateCO.setName(form.getName());
            evaTemplateCO.setDescription(form.getDescription());
        }
        //根据courseDO中的subjectId来查询课程对应的科目信息
        SubjectDO subjectDO = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", courseDO.getSubjectId()));
        if(subjectDO==null)throw new QueryException("该课程对应的科目不存在");
        //根据courseDO中的teacherId来查询课程对应的教师信息
        SysUserDO sysUserDO = userMapper.selectOne(new QueryWrapper<SysUserDO>().eq("id", courseDO.getTeacherId()));
        if(sysUserDO==null)throw new QueryException("该课程对应的教师不存在");
        //先根据课程ID来查询课程detail信息
        List<String> classRoomList = new ArrayList<>();
        List<CoursePeriod> courseTimeList = toGetCourseTime(id,classRoomList);
        //根据id查询课程类型表，得到课程类型的id集合
        List<Integer> courseTypeIds = courseTypeCourseMapper.selectList(new QueryWrapper<CourseTypeCourseDO>().eq("course_id", id)).stream().map(CourseTypeCourseDO::getTypeId).toList();
        //根据courseTypeIds查询课程类型表，得到课程类型的集合
        List<CourseTypeDO> courseTypes=new ArrayList<>();
        if(!courseTypeIds.isEmpty()){
             courseTypes = courseTypeMapper.selectList(new QueryWrapper<CourseTypeDO>().in(true, "id", courseTypeIds));
        }
        //将courseTypeDO转化成CourseType
        List<CourseType> courseTypeList = courseTypes.stream().map(courseConvertor::toCourseType).toList();
        //组装CourseDtailCO
        CourseDetailCO courseDetailCO = courseConvertor.toCourseDetailCO(courseTypeList
                , courseTimeList, subjectDO, courseDO, evaTemplateCO, sysUserDO,classRoomList);
        return Optional.of(courseDetailCO);
    }

    @Override
    public List<CourseScoreCO> findEvaScore(Integer id) {
        //根据课程ID找到全部courInfoDo信息
        List<Integer> courInfos = courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id", id)).stream().map(CourInfDO::getId).toList();
        if(courInfos.isEmpty()) throw new QueryException("该课程没有课程信息");
        //根据courInfos来找到评教任务id
        QueryWrapper<EvaTaskDO> evaTaskWrapper = new QueryWrapper<>();
        evaTaskWrapper.in(true,"cour_inf_id", courInfos);
        List<Integer> evaTaskDOIds = evaTaskMapper.selectList(evaTaskWrapper).stream().map(EvaTaskDO::getId).toList();
        if(evaTaskDOIds.isEmpty())throw new QueryException("暂时还没有该课程的评教任务");
        //根据评教任务id来找到评教表单记录数据中的form_props_values
        List<String> taskProps = formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id", evaTaskDOIds))
                .stream().map(FormRecordDO::getFormPropsValues).toList();
        if(taskProps.isEmpty())throw new QueryException("暂时还没有该课程的评教统计");
        //将json形式的字符串转化成EvaProp对象
        List<EvaProp> evaPropList = taskProps.stream().map(taskProp -> {
            try {
                return objectMapper.readValue(taskProp, EvaProp.class);
            } catch (JsonProcessingException e) {
                throw new ClassCastException("类型转化错误");
            }
        }).toList();
        //根据EvaProp中的prop进行分组
        Map<String, List<EvaProp>> evaPropMap = evaPropList.stream().collect(Collectors.groupingBy(EvaProp::getProp));
        List<CourseScoreCO> courseScoreCOList=new ArrayList<>();
        for (Map.Entry<String, List<EvaProp>> stringListEntry : evaPropMap.entrySet()) {
            CourseScoreCO courseScoreCO=new CourseScoreCO();
            Double sum=0.0;
            Double max=0.0;
            Double min=0.0;
            courseScoreCO.setProp(stringListEntry.getKey());
            for (EvaProp evaProp : stringListEntry.getValue()) {
                sum+=evaProp.getScore();
                if(evaProp.getScore()>max){
                    max= Double.valueOf(evaProp.getScore());
                }
                if(evaProp.getScore()<min){
                    min= Double.valueOf(evaProp.getScore());
                }
            }
            courseScoreCO.setAverScore( (sum/stringListEntry.getValue().size()));
            courseScoreCO.setMaxScore(max);
            courseScoreCO.setMinScore(min);
            courseScoreCOList.add(courseScoreCO);
        }
        return courseScoreCOList;
    }

    @Override
    @LocalCached(key = "#{@courseCacheConstants.SUBJECT_LIST}")
    public List<SubjectEntity> findSubjectInfo() {
        // 查询所有课程基础信息
        List<SubjectDO> subjectDOS = subjectMapper.selectList(null);
        if (subjectDOS.isEmpty()) return new ArrayList<>();
        //将数据库对象转换为业务对象
        return subjectDOS.stream().map(courseConvertor::toSubjectEntity).toList();
    }

    @Override
    public List<List<Integer>> getWeekCourses(Integer semId, Integer week) {
        //根据学期semId来找到这学期所有的课程ids
        List<Integer> courseIds = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semId)).stream().map(CourseDO::getId).toList();
        if (courseIds.isEmpty())throw new QueryException("暂时还没有该学期的课程信息");
        //根据学期semId和周数week来查询课程表（CourInfDO）再根据day来分组
        List<CourInfDO> list = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id", courseIds).eq("week", week));
        Map<Integer,List<CourInfDO>> map=new HashMap<>();
        //初始化map集合
        for (int i = 1; i <= 7; i++) {
            map.put(i,new ArrayList<>());
        }
        //将courInfDO集合添加到map集合中
        for (CourInfDO  courInfo : list){
            map.get(courInfo.getDay()).add(courInfo);
        }
        List<List<Integer>> result=new ArrayList<>();
        //每一天可能有6节大课，每一节大课开始时间一样
        for (int i=1;i<=6;i++){
            List<Integer> newList=new ArrayList<>();
            int finalI = i;
            for(int j=1;j<=7;j++){
                int sum= (int) map.get(j).stream().filter(courInfDO -> courInfDO.getStartTime() == 2 * finalI - 1).count();
                newList.add(sum);
            }
            result.add(newList);
        }
        return result;
    }

    @Override
    public List<SingleCourseCO> getPeriodInfo(Integer semId, CourseQuery courseQuery) {
        QueryWrapper<CourInfDO> wrapper = new QueryWrapper<>();
        wrapper.eq("week",courseQuery.getWeek())
                .eq("day",courseQuery.getDay())
                .and(wrapper1->wrapper1.eq("start_time",courseQuery.getNum())
                        .or()
                        .eq("start_time",courseQuery.getNum()+1));
        //先根据courseQuery中的信息来查询courInfoDO列表(课程详情表)

        List<CourInfDO> courInfDOS = courInfMapper.selectList(wrapper);
//        if(courInfDOS.isEmpty())throw new QueryException("暂时还没有该课程信息");
        //根据courseInfDo中的课程id和semid来筛选课程实体
        List<Integer> list = courInfDOS.stream().map(CourInfDO::getCourseId).toList();
        List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semId).in(!list.isEmpty(),"id",list));
        List<SingleCourseCO> singleCourseCOList = new ArrayList<>();
        //遍历courseDo组装SingleCourseCo
        for (CourInfDO courInfDO : courInfDOS) {
            CourseDO courseDO = getCourseInfo(courInfDO, courseDOS);
            SingleCourseCO singleCourseCO = new SingleCourseCO();
            singleCourseCO.setId(courInfDO.getId());
            singleCourseCO.setLocation(courInfDO.getLocation());
            CourseTime courseTime = new CourseTime().setWeek(courseQuery.getWeek()).setDay(courseQuery.getDay()).setStartTime(courInfDO.getStartTime()).setEndTime(courInfDO.getEndTime());
            singleCourseCO.setTime(courseTime);
            singleCourseCO.setTeacherName(userMapper.selectById(courseDO.getTeacherId()).getName());
            singleCourseCO.setName(subjectMapper.selectById(courseDO.getSubjectId()).getName());
            //根据课程id到评教任务表中统计数量
            singleCourseCO.setEvaNum(Math.toIntExact(evaTaskMapper.selectCount(new QueryWrapper<EvaTaskDO>()
                    .eq("cour_inf_id", courInfDO.getId())
                    .eq("status",0)
                    .or()
                    .eq("status",1)
                    .eq("cour_inf_id",courInfDO.getId())
            )));
            singleCourseCOList.add(singleCourseCO);
        }
        return singleCourseCOList;
    }

    @Override
    public Optional<SingleCourseEntity> getSingleCourseDetail(Integer id, Integer semId) {
        //ID是课程详情id
        CourInfDO courInfDO = courInfMapper.selectById(id);
        if(courInfDO==null){
            throw new QueryException("暂时还没有该课程详情信息");
        }
        //构建courseEntity
        CourseEntity courseEntity = toCourseEntity(courInfDO.getCourseId(), semId);

        return Optional.of(courseConvertor.toSingleCourseEntity(()->courseEntity,courInfDO));
    }

    @Override
    public List<RecommendCourseCO> getPeriodCourse(Integer semId, MobileCourseQuery courseQuery,String userName) {
        List<RecommendCourseCO> recommendCourseCOS = courseRecommendExce.togetPeriodCourse(semId, courseQuery, userName);

        if(courseQuery.getSort()==1){
            //时间升序排序
            // 创建流
            Stream<RecommendCourseCO> stream = recommendCourseCOS.stream();

            // 按照 time 的 week 属性进行升序排序
            Comparator<RecommendCourseCO> weekComparator = Comparator.comparing(course -> course.getTime().getWeek());

            // 如果 week 相同，则按 time 的 day 属性进行升序排序
            Comparator<RecommendCourseCO> dayComparator = Comparator.comparing(course -> course.getTime().getDay());
            Comparator<RecommendCourseCO> startComparator = Comparator.comparing(course -> course.getTime().getStartTime());

            // 组合比较器
            Comparator<RecommendCourseCO> combinedComparator = weekComparator.thenComparing(dayComparator);
            Comparator<RecommendCourseCO> starttoComparator = combinedComparator.thenComparing(startComparator);

            // 排序
            Stream<RecommendCourseCO> sortedStream = stream.sorted(starttoComparator);

            // 收集排序后的列表
            return sortedStream.toList();
        }else if(courseQuery.getSort()==2) {
            //时间降序排序
            // 时间降序排序
            Stream<RecommendCourseCO> stream = recommendCourseCOS.stream();

            // 按照 time 的 week 属性进行升序排序
            Comparator<RecommendCourseCO> weekComparator = Comparator.comparing(RecommendCourseCO::getTime, Comparator.comparing(CourseTime::getWeek)).reversed();

            // 如果 week 相同，则按 time 的 day 属性进行降序排序
            Comparator<RecommendCourseCO> dayComparator = Comparator.comparing(RecommendCourseCO::getTime, Comparator.comparing(CourseTime::getDay)).reversed();
            Comparator<RecommendCourseCO> startComparator = Comparator.comparing(RecommendCourseCO::getTime, Comparator.comparing(CourseTime::getStartTime)).reversed();

            // 组合比较器
            Comparator<RecommendCourseCO> combinedComparator = weekComparator.thenComparing(dayComparator);
            Comparator<RecommendCourseCO> starttoComparator = combinedComparator.thenComparing(startComparator);

            // 排序
            Stream<RecommendCourseCO> sortedStream = stream.sorted(starttoComparator);

            // 收集排序后的列表
            return sortedStream.toList();
        }else if(courseQuery.getSort()==0){
            Stream<RecommendCourseCO> stream = recommendCourseCOS.stream();
            Comparator<RecommendCourseCO> priorityComparator = Comparator.comparing(RecommendCourseCO::getEvaNum);
            Comparator<RecommendCourseCO> weekComparator = Comparator.comparing(course -> course.getTime().getWeek());
            Comparator<RecommendCourseCO> dayComparator = Comparator.comparing(course -> course.getTime().getDay());
            Comparator<RecommendCourseCO> startComparator = Comparator.comparing(course -> course.getTime().getStartTime());
            Comparator<RecommendCourseCO> recommendCourseCOComparator = priorityComparator.thenComparing(weekComparator);
            Comparator<RecommendCourseCO> recommendCourseCOComparator1 = recommendCourseCOComparator.thenComparing(dayComparator);
            Comparator<RecommendCourseCO> recommendCourseCOComparator2 = recommendCourseCOComparator1.thenComparing(startComparator);

            return stream.sorted(recommendCourseCOComparator2).toList();
        }else{
            return recommendCourseCOS;
        }

        }


    @Override
    public PaginationResultEntity<CourseTypeEntity> pageCourseType(PagingQuery<GenericConditionalQuery> courseQuery) {
        if(courseQuery==null){
            PaginationResultEntity<CourseTypeEntity> getCache=cacheManager.getCache(courseCacheConstants.COURSE_TYPE_LIST);
            if(getCache==null){
                List<CourseTypeDO> courseTypeDOS = courseTypeMapper.selectList(null);
                PaginationResultEntity<CourseTypeEntity> paginationEntity = paginationConverter
                        .toPaginationEntity(new Page<>(1, courseTypeDOS.size()), courseTypeDOS.stream().map(courseConvertor::toCourseTypeEntity).toList());
                cacheManager.putCache(courseCacheConstants.COURSE_TYPE_LIST,paginationEntity);
                return paginationEntity;
            }else{
                return getCache;
            }
        }
        Page<CourseTypeDO> page =new Page<>(courseQuery.getPage(),courseQuery.getSize());
//        QueryWrapper<CourseTypeDO> queryWrapper = new QueryWrapper<>();
        LambdaQueryWrapper<CourseTypeDO> queryWrapper = Wrappers.lambdaQuery();
        if(courseQuery.getQueryObj().getKeyword()!=null&&!courseQuery.getQueryObj().getKeyword().isEmpty()){
            queryWrapper.like(CourseTypeDO::getName,courseQuery.getQueryObj().getKeyword());
        }
//        QueryUtils.fileTimeQuery(queryWrapper,courseQuery.getQueryObj());
        QueryUtils.fileTimeQuery(queryWrapper,courseQuery.getQueryObj(),CourseTypeDO::getCreateTime,CourseTypeDO::getUpdateTime);

        //分页查询
        Page<CourseTypeDO> courseTypeDOPage = courseTypeMapper.selectPage(page, queryWrapper);
        List<CourseTypeDO> records = courseTypeDOPage.getRecords();
        List<CourseTypeEntity> list = records.stream().map(courseConvertor::toCourseTypeEntity).toList();
        return paginationConverter.toPaginationEntity(courseTypeDOPage,list);
    }

    @Override
    public List<List<SingleCourseEntity>> getUserCourseDetail(Integer id, Integer semId) {
        //id为用户id

        //构造semester
        SemesterEntity semesterEntity = getSemester(semId);
        if(semesterEntity==null)throw new QueryException("未找到对应学期");
        //构造courseDo
        List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>()
                .eq("teacher_id", id)
                .eq("semester_id", semId));
        if(courseDOS.isEmpty())return new ArrayList<>(new ArrayList<>());
        //构造userEntity
        UserEntity userEntity =toUserEntity(id);
        //根据courseDo中的subjectid找出courInfoDo集合
        Map<CourseDO, List<CourInfDO>> map=new HashMap<>();
        for (CourseDO courseDO : courseDOS) {
            map.put(courseDO, courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id", courseDO.getId())));
        }
        List<List<SingleCourseEntity>> list =new ArrayList<>();

        for (Map.Entry<CourseDO, List<CourInfDO>> courseDOListEntry : map.entrySet()) {
            List<SingleCourseEntity> temp = new ArrayList<>();
            SubjectEntity subjectEntity = courseConvertor.toSubjectEntity(subjectMapper.selectById(courseDOListEntry.getKey().getSubjectId()));
            for (CourInfDO courInfDO : courseDOListEntry.getValue()) {
                CourseEntity courseEntity = courseConvertor.toCourseEntity(courseDOListEntry.getKey(), () -> subjectEntity, () -> userEntity, () -> semesterEntity);
                SingleCourseEntity singleCourseEntity = courseConvertor.toSingleCourseEntity(() -> courseEntity, courInfDO);
                temp.add(singleCourseEntity);
            }
            list.add(temp);
        }
        return list;
    }

    /**
     * 获取自己教学的课程基础信息/获取自己所有教学的课程的详细信息
     *  @param semId 学期id
     *  @param userName 用户名
     * @return List<SelfTeachCourseCO>
     * */
    @Override
    public List<SelfTeachCourseCO> getSelfCourseInfo(String userName, Integer semId) {
        //根据用户名来查出教师id
        SysUserDO user = userMapper.selectOne(new QueryWrapper<SysUserDO>().eq("username", userName));
        if(user==null)throw new QueryException("用户不存在");
        Integer teacherId = user.getId();
        //根据学期来找到这学期所有课程
        List<CourseDO> courseList = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semId).eq("teacher_id", teacherId));
        if(courseList.isEmpty())return new ArrayList<>();
        List<SelfTeachCourseCO> list = new ArrayList<>();
        for (CourseDO courseDO : courseList) {

            //先拿到subjectDO
            SubjectDO subjectDO = subjectMapper.selectById(courseDO.getSubjectId());
            if (subjectDO==null)throw new QueryException("未找到对应科目");
            //拿到课程类型id集合
            List<CourseTypeCourseDO> courseTypeCourse = courseTypeCourseMapper.selectList(new QueryWrapper<CourseTypeCourseDO>().eq("course_id", courseDO.getId()));
//            if(courseTypeCourse.isEmpty())throw new QueryException("未找到对应课程类型");
            List<Integer> courseTypeIds =courseTypeCourse .stream().map(CourseTypeCourseDO::getTypeId).toList();
            List<CourseTypeDO> typeList = courseTypeIds.stream().map(courseTypeMapper::selectById).toList();
            List<CourseType> TPList = typeList.stream().map(courseConvertor::toCourseType).toList();
            //统计评教数
            Long courInfId = evaTaskMapper.selectCount(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courseDO.getId()));
            //添加到selfteachCourseCO中
            SelfTeachCourseCO selfTeachCourseCO = new SelfTeachCourseCO();
            selfTeachCourseCO.setId(courseDO.getId())
                            .setName(subjectDO.getName())
                            .setTypeList(TPList)
                            .setNature(subjectDO.getNature())
                            .setEvaNum(courInfId.intValue());
            list.add(selfTeachCourseCO);
        }
        return list;
    }


    @Override
    public List<RecommendCourseCO> getSelfCourse(Integer semId, String userName){
        //得到当前是第几周
        CourseTime courseTime = toCourseTime(semId);
        return courseRecommendExce.RecommendCourse(semId,userName,courseTime);
    }



    @Override
    public List<SingleCourseEntity> getSelfCourseTime( Integer id) {
        //先根据课程id来查出课程实体
        CourseDO courseDO = courseMapper.selectById(id);
        if(courseDO==null)return new ArrayList<>();
        //先得到CourseEntity
        CourseEntity courseEntity = toCourseEntity(id,courseDO.getSemesterId());
        //根据id来查出CourInfoDo集合
        List<CourInfDO> courInfDOS = courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id", id));
        if(courInfDOS.isEmpty())throw new QueryException("未找到对应课程信息");
        //转化成SingleCourseEntity
        return courInfDOS.stream().map(courInfDO -> courseConvertor.toSingleCourseEntity(()->courseEntity, courInfDO)).toList();
    }

    @Override
    public String getDate(Integer semId, Integer week, Integer day) {
        SemesterDO semesterDO = semesterMapper.selectById(semId);
        if(semesterDO==null)throw new QueryException("未找到对应学期");
        LocalDate localDate = semesterDO.getStartDate().plusDays((week-1) * 7L + day - 1);
        return localDate.toString();
    }

    @Override
    public List<String> getLocation(Integer courseId) {
        //并对教室去重
        return courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id", courseId))
                .stream().map(CourInfDO::getLocation).distinct().toList();
    }

    @Override
    public List<CourseType> getCourseType(Integer courseId) {
        List<CourseTypeCourseDO> courseTypeCourse = courseTypeCourseMapper.selectList(new QueryWrapper<CourseTypeCourseDO>().eq("course_id", courseId));
        if (courseTypeCourse.isEmpty())return new ArrayList<>();
        List<Integer> typeIds = courseTypeCourse.stream().map(CourseTypeCourseDO::getTypeId).toList();
       return courseTypeMapper.selectList(new QueryWrapper<CourseTypeDO>().in(!typeIds.isEmpty(),"id", typeIds)).stream().map(courseConvertor::toCourseType).toList();
    }

    @Override
    public List<EvaTeacherInfoCO> getEvaUsers(Integer courseId) {
        List<EvaTaskDO> taskDOList = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courseId));
        if (taskDOList.isEmpty())return new ArrayList<>();
        List<Integer> userList = taskDOList.stream().map(EvaTaskDO::getTeacherId).toList();
        return userMapper.selectList(new QueryWrapper<SysUserDO>().in(!userList.isEmpty(),"id", userList)).stream().map(courseConvertor::toEvaTeacherInfoCO).toList();
    }

    @Override
    public Optional<CourseEntity> getCourseByInfo(Integer courInfId) {
        CourInfDO courInfDO=courInfMapper.selectById(courInfId);
        CourseDO courseDO=courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id",courInfId));

        Supplier<UserEntity> userEntity=()->toUserEntity(courseDO.getTeacherId());
        Supplier<SemesterEntity> semesterEntity=()->courseConvertor.toSemesterEntity(semesterMapper.selectById(courseDO.getSemesterId()));
        Supplier<SubjectEntity> subjectEntity=()->courseConvertor.toSubjectEntity(subjectMapper.selectById(courseDO.getSubjectId()));
        CourseEntity courseEntity=courseConvertor.toCourseEntity(courseDO,subjectEntity,userEntity,semesterEntity);

        return Optional.of(courseEntity);
    }

    @Override
    public Optional<CourseTime> getCourseTimeByCourse(Integer courInfId) {
        CourInfDO courInfDO=courInfMapper.selectById(courInfId);
        CourseTime courseTime=new CourseTime();
        courseTime.setStartTime(courInfDO.getStartTime());
        courseTime.setEndTime(courInfDO.getEndTime());
        courseTime.setWeek(courInfDO.getWeek());
        courseTime.setDay(courInfDO.getDay());
        return Optional.of(courseTime);
    }


    private SemesterEntity getSemester(Integer semId){
        SemesterDO semesterDO=null;
        if(semId!=null){
            semesterDO= semesterMapper.selectById(semId);
            if(semesterDO==null)throw new QueryException("学期不存在");
            return courseConvertor.toSemesterEntity(semesterDO);
        }
        return null;
    }
    private List<CoursePeriod> toGetCourseTime( Integer id,List<String> list){
        QueryWrapper<CourInfDO> queryWrapper = Wrappers.query();
        queryWrapper.eq("course_id", id);
        // 执行查询
        List<CourInfDO> resultList = courInfMapper.selectList(queryWrapper);
        if(resultList.isEmpty())throw new QueryException("还没有该课程对应的授课");
        // 处理查询结果
        Map<Integer, List<CourInfDO>> groupedByDay = resultList.stream()
                .collect(Collectors.groupingBy(CourInfDO::getDay));
        List<CoursePeriod> finalResult = new ArrayList<>();
        for (Map.Entry<Integer, List<CourInfDO>> entry : groupedByDay.entrySet()) {
            Integer day = entry.getKey();
            List<CourInfDO> courInfDOS = entry.getValue();
            // 找到最大 week 的数据
            CourInfDO maxWeek = courInfDOS.stream()
                    .max(Comparator.comparingInt(CourInfDO::getWeek))
                    .orElse(null);

            // 找到最小 week 的数据
            CourInfDO minWeek = courInfDOS.stream()
                    .min(Comparator.comparingInt(CourInfDO::getWeek))
                    .orElse(null);
            if(maxWeek!=null&&minWeek!=null){
                CoursePeriod coursePeriod = new CoursePeriod();
                coursePeriod.setStartWeek(minWeek.getWeek());
                coursePeriod.setEndWeek(maxWeek.getWeek());
                coursePeriod.setDay(day);
                coursePeriod.setStartTime(minWeek.getStartTime());
                coursePeriod.setEndTime(maxWeek.getEndTime());
                finalResult.add(coursePeriod);
                if(!list.contains(maxWeek.getLocation())){
                    list.add(maxWeek.getLocation());
                }
                if(!list.contains(minWeek.getLocation())){
                    list.add(minWeek.getLocation());
                }
            }


        }
        return finalResult;

    }
    private CourseDO getCourseInfo( CourInfDO courInfDO,List<CourseDO> list){
        for (CourseDO course : list) {
            if(course.getId().equals(courInfDO.getCourseId())){
                return course;
            }
        }
        return new CourseDO();
    }
    private CourseEntity toCourseEntity(Integer courseId,Integer semId){
        //构造semester
        SemesterEntity semesterEntity = getSemester(semId);
        //构造courseDo
        CourseDO courseDO = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", courseId).eq("semester_id", semId));
        if(courseDO==null) throw new QueryException("并未找到相关课程");
        //构造subject
        SubjectDO subjectDO = subjectMapper.selectById(courseDO.getSubjectId());
        if (subjectDO==null) throw new QueryException("并未找到对应科目");
        SubjectEntity subjectEntity = courseConvertor.toSubjectEntity(subjectDO);
        //构造userEntity
        UserEntity userEntity =toUserEntity(courseDO.getTeacherId());
        return courseConvertor.toCourseEntity(courseDO,()->subjectEntity,()->userEntity,()->semesterEntity);
    }
    private UserEntity toUserEntity(Integer userId){
        //得到uer对象
        SysUserDO userDO = userMapper.selectById(userId);
        if(userDO==null)throw new QueryException("并未找到相关用户");
        //根据userId找到角色id集合
        List<Integer> roleIds = userRoleMapper.selectList(new QueryWrapper<SysUserRoleDO>().eq("user_id", userId)).stream().map(SysUserRoleDO::getRoleId).toList();
        //根据角色id集合找到角色对象集合
        Supplier<List<RoleEntity>> roleEntities= ArrayList::new;
        if(!roleIds.isEmpty()){
            roleEntities =()-> roleMapper.selectList(new QueryWrapper<SysRoleDO>().in(true,"id", roleIds)).stream().map(roleConverter::toRoleEntity).toList();
        }
        //根据角色id集合找到角色菜单表中的菜单id集合
//        List<Integer> menuIds = roleMenuMapper.selectList(new QueryWrapper<SysRoleMenuDO>().in("role_id", roleIds)).stream().map(SysRoleMenuDO::getMenuId).toList();
        //根据menuids找到菜单对象集合
//        List<MenuEntity> menuEntities = menuMapper.selectList(new QueryWrapper<SysMenuDO>().in("id", menuIds)).stream().map(menuDO -> menuConvertor.toMenuEntity(menuDO)).toList();


        return userConverter.toUserEntity(userDO,roleEntities);
    }
    private LocalDateTime toLocalDateTime(String s){

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(s, formatter);
        return localDate.atStartOfDay();
    }
    private void toJudge(MobileCourseQuery courseQuery,QueryWrapper wrapper){
        if(courseQuery.getDepartmentName()!=null&&courseQuery.getTeacherId()!=null){
            wrapper.eq("department",courseQuery.getDepartmentName());
            wrapper.eq("id",courseQuery.getTeacherId());
        } else if (courseQuery.getDepartmentName()!=null&&courseQuery.getTeacherId()==null) {
            wrapper.eq("department",courseQuery.getDepartmentName());
        } else if (courseQuery.getTeacherId()!=null&&courseQuery.getDepartmentName()==null) {
            wrapper.eq("id",courseQuery.getTeacherId());
        }else{
            wrapper=null;
        }


    }
    private void toJudgeTime(MobileCourseQuery courseQuery,QueryWrapper wrapper){
        if(courseQuery.getStartDay()!=null&&courseQuery.getEndDay()!=null){
            wrapper.between("start_time",courseQuery.getStartDay(),courseQuery.getEndDay());
        }else if(courseQuery.getStartDay()!=null&&courseQuery.getEndDay()==null){
            wrapper.ge("start_time",courseQuery.getStartDay());
        }else if(courseQuery.getStartDay()==null&&courseQuery.getEndDay()!=null){
            wrapper.le("end_time",courseQuery.getEndDay());
        }else{
            wrapper=null;
        }

    }
    public CourseTime toCourseTime(Integer semId){
        SemesterDO semesterDO = semesterMapper.selectById(semId);
        if(semesterDO==null){
            throw new  QueryException("学期不合理");
        }
        LocalDateTime now=LocalDateTime.now();
        int start=courseRecommendExce.toGetStartTime(now);
        long diff = ChronoUnit.DAYS.between(semesterDO.getStartDate(), now);
        // 计算当前周数
        int currentWeek = (int) (diff / 7) + 1;
        //计算当前星期几
        int currentDayOfWeek = now.getDayOfWeek().getValue();
        return new CourseTime().setWeek(currentWeek).setDay(currentDayOfWeek).setStartTime(start);


    }
/*    private List<RecommendCourseCO> getRecommendCourInfo(List<CourInfDO> courInfoList,List<CourseDO> courInfDOS) {
        //找到courInfoList中courInfoDo的id不在evaTask的cour_inf_id中的数据
        List<Integer> checkList = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("status",0).or().eq("status",1)).stream().map(EvaTaskDO::getCourInfId).toList();
        Stream<CourInfDO> notExistCourInfo = courInfoList.stream().filter(courInfDO -> !checkList.contains(courInfDO.getId()));
        //被选过的课程（从courInfoList中去除notExistCourInfo）
        List<CourInfDO> existCourInfo = courInfoList.stream().filter(courInfDO -> checkList.contains(courInfDO.getId())).toList();
        //根据未被选过的课程的courseId来分类
        Map<Integer, List<CourInfDO>> map = notExistCourInfo.collect(Collectors.groupingBy(CourInfDO::getCourseId));
        Map<Integer,List<Integer>> mapCourse= new HashMap<>();
        for (Map.Entry<Integer, List<CourInfDO>> integerListEntry : map.entrySet()) {
            CourseDO courseDO = courseMapper.selectById(integerListEntry.getKey());
            if(!mapCourse.containsKey(courseDO.getTeacherId())){
                ArrayList<Integer> list = new ArrayList<>();
                list.add(integerListEntry.getKey());
                mapCourse.put(courseDO.getTeacherId(),list);
            }else{
                mapCourse.get(courseDO.getTeacherId()).add(integerListEntry.getKey());
            }
        }
        //老师所有课程都没有被选的（优先级priority: 5）；
        List<CourInfDO> noCourInfoList=new ArrayList<>();
        //没有被选择过得课程（优先级priority: 2）；
        List<CourInfDO> notSelectCourInfoList=new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entry : mapCourse.entrySet()) {
            if(entry.getValue().size()>1){
                for (Integer i : entry.getValue()) {
                    noCourInfoList.addAll(map.get(i));
                }
            }else{
                notSelectCourInfoList.addAll(map.get(entry.getValue().get(0)));
            }
        }
        List<RecommendCourseCO> recommendCourInfo = getRecommendCourInfo(noCourInfoList, 5, courInfDOS);
        recommendCourInfo.addAll(getRecommendCourInfo(notSelectCourInfoList,2,courInfDOS));
        recommendCourInfo.addAll(getRecommendCourInfo(existCourInfo,-2,courInfDOS));



        return recommendCourInfo;
    }*/
/*    private List<RecommendCourseCO> getRecommendCourInfo(List<CourInfDO> courInfoList,Integer priority,List<CourseDO> courInfDOS){
        //先根据课程id进行分类
        Map<Integer, List<CourInfDO>> map = courInfoList.stream().collect(Collectors.groupingBy(CourInfDO::getCourseId));
        List<RecommendCourseCO> list=new ArrayList<>();
        for (Map.Entry<Integer, List<CourInfDO>> entry : map.entrySet()){
            CourseDO courseDO = courseMapper.selectById(entry.getKey());
            //获取SubjectDO
            SubjectDO subjectDO = subjectMapper.selectById(courseDO.getSubjectId());
            //获取SysUserDO
            SysUserDO sysUserDO = userMapper.selectById(courseDO.getTeacherId());
            //获取课程类型和获取课程类型相似度
            Map<Double, List<CourseType>> getCourseTypeList = toGetCourseTypeList(entry.getKey(), courInfDOS);
            //教学老师被评教的次数
            Integer allEvaNum = getEvaNum(courseDO.getTeacherId(), courseDO.getSemesterId());
            for (CourInfDO courInfDO : entry.getValue()) {
                //获取评教数量
                Integer evaNum = Math.toIntExact(evaTaskMapper.selectCount(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courInfDO.getId())));
                //获取CourseTime
                CourseTime courseTime = courseConvertor.toCourseTime(courInfDO);
                for (Map.Entry<Double, List<CourseType>> entrySet : getCourseTypeList.entrySet()) {
                    RecommendCourseCO recommendCourseCO = courseConvertor.toRecommendCourseCO(courInfDO, subjectDO, sysUserDO, evaNum, courseTime, allEvaNum, entrySet.getValue(), priority + 4 * entrySet.getKey(), entrySet.getKey());
                    list.add(recommendCourseCO);
                }

            }


        }
        return list;
    }*/
    private  Map<Double,List<CourseType>> toGetCourseTypeList(Integer courseId,List<CourseDO> courInfDOS){
        double score=0.0;
        //待听的课的课程类型
        List<Integer> typeList = courseTypeCourseMapper.selectList(new QueryWrapper<CourseTypeCourseDO>().eq("course_id", courseId)).stream().map(CourseTypeCourseDO::getTypeId).toList();
        //老师所教学课程的课程类型
        List<Integer> teacherTypeList=new ArrayList<>();
        for (CourseDO courInfDO : courInfDOS) {
            teacherTypeList.addAll(courseTypeCourseMapper.selectList(new QueryWrapper<CourseTypeCourseDO>().eq("course_id", courInfDO.getId())).stream().map(CourseTypeCourseDO::getTypeId).toList());
        }
        //获取到teacherTypeList与typeList的交集
        List<Integer> list = teacherTypeList.stream().filter(typeList::contains).toList();
        if(typeList.size()>=teacherTypeList.size()){
            score=(double) list.size()/typeList.size();
        }else{
            score=(double) list.size()/teacherTypeList.size();
        }
        Map<Double,List<CourseType>> map=new HashMap<>();
        List<CourseType> typeList1 = courseTypeMapper.selectBatchIds(typeList).stream().map(courseConvertor::toCourseType).toList();

        map.put(score,typeList1);

        return map;
    }
    private Integer  getEvaNum(Integer teacherId,Integer semId){
        List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", teacherId).eq("semester_id", semId));
        List<Integer> courseId = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in(!courseDOS.isEmpty(),"course_id", courseDOS)).stream().map(CourInfDO::getId).toList();
        Long courInfId = evaTaskMapper.selectCount(new QueryWrapper<EvaTaskDO>().in(!courseId.isEmpty(),"cour_inf_id", courseId));

        return courInfId.intValue();
    }

    private PaginationResultEntity<CourseEntity> getCourseList(Integer semId) {
        PaginationResultEntity<CourseEntity> result=new PaginationResultEntity<>();
//        Page<SysUserDO> pageUser=new Page<>(courseQuery.getPage(),courseQuery.getSize())
        List<CourseEntity> list=new ArrayList<>();
        //获取所有的课程的基础信息
        List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semId));
        if(courseDOS.isEmpty()){
            result.setRecords(new ArrayList<>());
            return result;
        }
        //根据科目id进行分类
        Map<Integer, List<CourseDO>> map = courseDOS.stream().collect(Collectors.groupingBy(CourseDO::getSubjectId));
        for (Map.Entry<Integer, List<CourseDO>> entry : map.entrySet()) {
            SubjectDO subjectDO = subjectMapper.selectById(entry.getKey());
//            SubjectEntity subject=SpringUtil.getBean(SubjectEntity.class);
            SubjectEntity subject=new SubjectEntity();
            subject.setName(subjectDO.getName());
            subject.setNature(subjectDO.getNature());
            for (CourseDO courseDO : entry.getValue()) {
                CourseEntity entity= new CourseEntity();
//                CourseEntity entity = courseConvertor.toCourseEntity(courseDO, () -> null, () -> null, () -> null);
                entity.setId(courseDO.getId());
                entity.setSubject(() -> subject);
                SysUserDO sysUserDO = userMapper.selectById(courseDO.getTeacherId());

                UserEntity user = SpringUtil.getBean(UserEntity.class);
                user.setName(sysUserDO.getName());
                entity.setTeacher(()-> user);
                list.add(entity);

            }
        }

        result.setRecords(list);
        return result;

    }

}
