package edu.cuit.infra.gateway.impl.courseimpl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cuit.client.bo.EvaProp;

import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SingleCourseCO;
import edu.cuit.client.dto.clientobject.eva.CourseScoreCO;
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
import edu.cuit.infra.dal.database.dataobject.user.*;
import edu.cuit.infra.dal.database.mapper.course.*;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.user.*;
import edu.cuit.infra.util.QueryUtils;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
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
    private final CourOneEvaTemplateMapper CoureOneEvaTemplateMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final FormRecordMapper formRecordMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final PaginationConverter paginationConverter;
    @Override
    public PaginationResultEntity<CourseEntity> page(PagingQuery<CourseConditionalQuery> courseQuery, Integer semId) {
        // 根据courseQuery中的departmentName以及courseQuery中的页数和一页显示数到userMapper中找对应数量的对应用户id，
        List<Integer> userIds=null;
        if(courseQuery.getQueryObj().getDepartmentName()!=null){
            Page<SysUserDO> pageUser=new Page<>();
            pageUser=userMapper.selectPage(pageUser,new QueryWrapper<SysUserDO>().like("department",courseQuery.getQueryObj().getDepartmentName()));
            userIds=pageUser.getRecords().stream().map(SysUserDO::getId).toList();
        }
        //根据courseQuery中的create时间范围，和update时间范围查询semester_id为semId的CourseDO
        Page<CourseDO> pageCourse=new Page<>(courseQuery.getPage(),courseQuery.getSize());
        QueryWrapper<CourseDO> courseWrapper=new QueryWrapper<>();
        QueryUtils.fileTimeQuery(courseWrapper,courseQuery.getQueryObj());
        if(semId!=null){
            courseWrapper.eq("semester_id",semId);
        }
        if(userIds!=null){
            courseWrapper.in("teacher_id",userIds);
        }

        pageCourse = courseMapper.selectPage(pageCourse,courseWrapper);
        //将paginationEntity中的records类型转化成CourseEntity
        List<CourseDO> records = pageCourse.getRecords();
        List<CourseEntity> list = records.stream().map(courseDO -> toCourseEntity(courseDO.getId(), semId)).toList();
        PaginationResultEntity<CourseEntity> paginationEntity = paginationConverter.toPaginationEntity(pageCourse, list);


        return paginationEntity;
    }

    @Override
    public Optional<CourseDetailCO> getCourseInfo(Integer id, Integer semId) {
        //根据id和semId来查询课程信息
        CourseDO courseDO = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", id).eq("semester_id", semId));
        //根据id和semId来查询评教快照信息
        CourOneEvaTemplateDO courOneEvaTemplateDO = CoureOneEvaTemplateMapper.selectOne(new QueryWrapper<CourOneEvaTemplateDO>().eq("course_id", id).eq("semester_id", semId));
        //将courOneEvaTemplateDO中的formtemplate(json)字符串，转换为EvaTemplateCO
        EvaTemplateCO evaTemplateCO=null;
      if(courOneEvaTemplateDO.getFormTemplate()!=null){
          try {
              evaTemplateCO = new ObjectMapper().readValue(courOneEvaTemplateDO.getFormTemplate(), EvaTemplateCO.class);
          } catch (JsonProcessingException e) {
              throw new QueryException("formTemplate暂时为空");
          }
      }
        //根据courseDO中的subjectId来查询课程对应的科目信息
        SubjectDO subjectDO = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", courseDO.getSubjectId()));
       //根据courseDO中的teacherId来查询课程对应的教师信息
        SysUserDO sysUserDO = userMapper.selectOne(new QueryWrapper<SysUserDO>().eq("id", courseDO.getTeacherId()));
        //先根据课程ID来查询课程detail信息

        List<CoursePeriod> courseTimeList = toGetCourseTime(id);
        //根据id查询课程类型表，得到课程类型的id集合
        List<Integer> courseTypeIds = courseTypeCourseMapper.selectList(new QueryWrapper<CourseTypeCourseDO>().eq("course_id", id)).stream().map(CourseTypeCourseDO::getTypeId).toList();
        //根据courseTypeIds查询课程类型表，得到课程类型的集合
        List<CourseTypeDO> courseTypes = courseTypeMapper.selectList(new QueryWrapper<CourseTypeDO>().in("id", courseTypeIds));
        //将courseTypeDO转化成CourseType
        List<CourseType> courseTypeList = courseTypes.stream().map(courseTypeDO -> courseConvertor.toCourseType(id,courseTypeDO)).toList();

        //组装CourseDtailCO
        CourseDetailCO courseDetailCO = courseConvertor.toCourseDetailCO(courseTypeList
                , courseTimeList, subjectDO, courseDO, evaTemplateCO, sysUserDO);
        return Optional.of(courseDetailCO);

    }

    @Override
    public List<CourseScoreCO> findEvaScore(Integer id, Integer semId) {
        //根据课程详情ID找到全部courInfoDo信息
        List<Integer> courInfos = courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id", id)).stream().map(CourInfDO::getId).toList();
        //根据courInfos来找到评教任务id
        QueryWrapper<EvaTaskDO> evaTaskWrapper = new QueryWrapper<>();
        evaTaskWrapper.in("cour_inf_id", courInfos);
        List<Integer> evaTaskDOIds = evaTaskMapper.selectList(evaTaskWrapper).stream().map(EvaTaskDO::getId).toList();
        //根据评教任务id来找到评教表单记录数据中的form_props_values
        List<String> taskProps = formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id", evaTaskDOIds))
                .stream().map(formRecordDO -> formRecordDO.getFormPropsValues()).toList();
        //将json形式的字符串转化成EvaProp对象
        List<EvaProp> evaPropList = taskProps.stream().map(taskProp -> {
            try {
                return new ObjectMapper().readValue(taskProp, EvaProp.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
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
    public List<SubjectEntity> findSubjectInfo() {
        // 查询所有课程基础信息
        List<SubjectDO> subjectDOS = subjectMapper.selectList(null);
        //将数据库对象转换为业务对象
        List<SubjectEntity> subjectEntities = subjectDOS.stream().map(subjectDO -> courseConvertor.toSubjectEntity(subjectDO)).toList();
        return subjectEntities;
    }

    @Override
    public List<List<Integer>> getWeekCourses(Integer week, Integer semId) {
        //根据学期semId来找到这学期所有的课程ids
        List<Integer> courseIds = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semId)).stream().map(CourseDO::getId).toList();
        //根据学期semId和周数week来查询课程表（CourInfDO）再根据day来分组
        List<CourInfDO> list = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id", courseIds).eq("week", week));
        Map<Integer,List<CourInfDO>> map=new HashMap<>();
        //初始化map集合
        for (int i = 1; i <= 7; i++) {
            map.put(i,new ArrayList<>());
        }
        //将courInfDO集合添加到map集合中
        for (CourInfDO  courInfo : list) {
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
            newList.clear();
        }
        return result;
    }

    @Override
    public List<SingleCourseCO> getPeriodInfo(Integer semId, CourseQuery courseQuery) {
        QueryWrapper<CourInfDO> wrapper = new QueryWrapper<>();
        if(courseQuery.getDay()!=null){
            wrapper.eq("day", courseQuery.getDay());
        }
        if(courseQuery.getNum()!=null){
            wrapper.eq("start_time", courseQuery.getNum());
        }
        if(courseQuery.getWeek()!=null){
            wrapper.eq("week", courseQuery.getWeek());
        }
        //先根据courseQuery中的信息来查询courInfoDO列表(课程详情表)
        List<CourInfDO> courInfDOS = courInfMapper.selectList(wrapper);
        //根据courseInfDo中的课程id和semid来筛选课程实体
        List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semId).in("id", courInfDOS.stream().map(CourInfDO::getCourseId).toList()));

        List<SingleCourseCO> singleCourseCOList = new ArrayList<>();
        //遍历courseDo组装SingleCourseCo
        for (CourseDO courseDO : courseDOS) {
           SingleCourseCO singleCourseCO = new SingleCourseCO();
            CourInfDO courseInfo = getCourseInfo(courseDO);
            singleCourseCO.setId(courseInfo.getId());
            CourseTime courseTime = new CourseTime().setWeek(courseQuery.getWeek()).setDay(courseQuery.getDay()).setStartTime(courseQuery.getNum());
            if(courseQuery.getNum()!=null){
                courseTime.setEndTime(courseQuery.getNum()+1);
            }
            singleCourseCO.setTime(courseTime);
            singleCourseCO.setTeacherName(userMapper.selectById(courseDO.getTeacherId()).getName());
            singleCourseCO.setName(subjectMapper.selectById(courseDO.getSubjectId()).getName());
            //根据课程id到评教任务表中统计数量
            singleCourseCO.setEvaNum(Math.toIntExact(evaTaskMapper.selectCount(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courseInfo.getId()))));
            singleCourseCOList.add(singleCourseCO);
        }



        return singleCourseCOList;
    }

    @Override
    public Optional<SingleCourseEntity> getSingleCourseDetail(Integer id, Integer semId) {
        //ID是课程详情id
        CourInfDO courInfDO = courInfMapper.selectById(id);
        //构建courseEntity
        CourseEntity courseEntity = toCourseEntity(courInfMapper.selectById(id).getCourseId(), semId);

        return Optional.of(courseConvertor.toSingleCourseEntity(courseEntity,courInfDO));
    }

    @Override
    public List<SingleCourseEntity> getPeriodCourse(Integer semId, MobileCourseQuery courseQuery) {
        //先得到课程详情
        QueryWrapper<CourInfDO> courInfoWrapper=new QueryWrapper<>();
        toJudgeTime(courseQuery,courInfoWrapper);
        List<CourInfDO> courInfDOS = courInfMapper.selectList(courInfoWrapper);
        //得到courseDo
        List<Integer> userList=null;
        List<CourseDO> courseList=null;
        QueryWrapper<SysUserDO> wrapper = new QueryWrapper<>();
        toJudge(courseQuery,wrapper);
        if(wrapper!=null){
           userList=userMapper.selectList(wrapper).stream().map(user->user.getId()).toList();
          courseList = userList.stream().map(userId -> courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("teacher_id", userId).eq("semester_id", semId))).toList();
        }
        if(courseQuery.getKeyword()!=null){
            //得到科目集合并过滤courseList
            List<Integer> subjectIds = subjectMapper.selectList(new QueryWrapper<SubjectDO>().like("name", courseQuery.getKeyword())).stream().map(SubjectDO::getId).toList();
           if(courseList!=null){
               courseList=courseList.stream().filter(courseDO -> subjectIds.contains(courseDO.getSubjectId())).toList();
           }else{
               courseList=courseMapper.selectList(new QueryWrapper<CourseDO>().in("subject_id",subjectIds).eq("semester_id",semId));
           }
        }
        if(courseQuery.getTypeId()!=null){
            //根据Typeid，得到课程id
            List<Integer> courseIds = courseTypeCourseMapper.selectList(new QueryWrapper<CourseTypeCourseDO>()
                                                            .eq("type_id", courseQuery.getTypeId()))
                                                            .stream().map(CourseTypeCourseDO::getCourseId).toList();
            if(courseList!=null){
                courseList=courseList.stream().filter(courseDO -> courseIds.contains(courseDO.getId())).toList();
            }else{
                courseList=courseMapper.selectList(new QueryWrapper<CourseDO>().in("id",courseIds).eq("semester_id",semId));
            }
        }
        //得到课程id
        List<Integer> courseIds = courseList.stream().map(CourseDO::getId).toList();
        //过滤courInfoDOS中的course_id在courseIds中的
        List<CourInfDO> courInfDOS1 = courInfDOS.stream().filter(courInfDO -> courseIds.contains(courInfDO.getCourseId())).toList();
        //得到CourseEntity对象集合
        Map<CourInfDO, CourseEntity> map=new HashMap<>();
       //先将courInfDOS1根据courseId分组
        Map<Integer, List<CourInfDO>> collect = courInfDOS1.stream().collect(Collectors.groupingBy(CourInfDO::getCourseId));
        for (Integer courseId : collect.keySet()) {
            CourseEntity courseEntity = toCourseEntity(courseId, semId);
            for (CourInfDO courInfDO : collect.get(courseId)) {
                map.put(courInfDO, courseEntity);
            }
        }

        //得到SingleCourseEntity对象集合

        List<SingleCourseEntity> list = new ArrayList<>();
        for (CourInfDO courInfDO : map.keySet()) {
            list.add(courseConvertor.toSingleCourseEntity(map.get(courInfDO), courInfDO));
        }
      return list;

    }

    @Override
    public PaginationResultEntity<CourseTypeEntity> pageCourseType(PagingQuery<GenericConditionalQuery> courseQuery) {
        Page<CourseTypeDO> page =new Page<>(courseQuery.getPage(),courseQuery.getSize());
        QueryWrapper<CourseTypeDO> queryWrapper = new QueryWrapper<>();
            if(courseQuery.getQueryObj().getKeyword()!=null){
                queryWrapper.like("name",courseQuery.getQueryObj().getKeyword());
            }
           QueryUtils.fileTimeQuery(queryWrapper,courseQuery.getQueryObj());
            Page<CourseTypeDO> courseTypeDOPage = courseTypeMapper.selectPage(page, queryWrapper);
            List<CourseTypeDO> records = courseTypeDOPage.getRecords();
        List<CourseTypeEntity> list = records.stream().map(courseTypeDO -> courseConvertor.toCourseTypeEntity(courseTypeDO)).toList();
        return paginationConverter.toPaginationEntity(courseTypeDOPage,list);
    }

    @Override
    public List<SingleCourseEntity> getUserCourseDetail(Integer id, Integer semId) {
        //id为用户id

        //构造semester
        SemesterEntity semesterEntity = getSemester(semId);
        //构造courseDo
        List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>()
                .eq("teacher_id", id)
                .eq("semester_id", semId));

        List<Integer> subjectIds = courseDOS.stream().map(CourseDO::getSubjectId).toList();
        for (CourseDO courseDO : courseDOS) {
            subjectIds.add(courseDO.getSubjectId());
        }
        List<SubjectDO> subjectDOS = subjectMapper.selectList(new QueryWrapper<SubjectDO>().in("id", subjectIds));
        //构造subject
        List<SubjectEntity> subjectEntitys= subjectDOS.stream().map(subjectDO -> courseConvertor.toSubjectEntity(subjectDO)).toList();
        //构造userEntity
        UserEntity userEntity =toUserEntity(id);
        //根据courseDo中的subjectid找出courInfoDo集合
        Map<CourseDO, List<CourInfDO>> map=new HashMap<>();
        for (CourseDO courseDO : courseDOS) {
            map.put(courseDO, courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id", courseDO.getId())));
           }
        List<SingleCourseEntity> list =new ArrayList<>();
        for (CourseDO courseDO : map.keySet()) {
            //得到subjectEntity中ID等于courseDO.getSubjectId()的subjectEntity
            SubjectEntity subjectEntity = subjectEntitys.stream().filter(subject -> subject.getId().equals(courseDO.getSubjectId())).findFirst().get();
            for (CourInfDO courInfDO : map.get(courseDO)) {
               list.add(courseConvertor.toSingleCourseEntity(courseConvertor.toCourseEntity(courseDO, subjectEntity,userEntity,semesterEntity), courInfDO));
            }
        }


        return list;
    }


    @Override
    public List<SelfTeachCourseCO> getSelfCourseInfo(String userName, Integer semId) {
        //根据用户名来查出教师id
        Integer teacherId = userMapper.selectOne(new QueryWrapper<SysUserDO>().eq("username", userName)).getId();

        //根据学期来找到这学期所有课程
        List<CourseDO> courseList = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semId).eq("teacher_id", teacherId));
        //
        List<SelfTeachCourseCO> list = new ArrayList<>();
        for (CourseDO courseDO : courseList) {
            SelfTeachCourseCO selfTeachCourseCO = new SelfTeachCourseCO();
            //先拿到subjectDO
            SubjectDO subjectDO = subjectMapper.selectById(courseDO.getSubjectId());
            //拿到课程类型id集合
            List<Integer> courseTypeIds = courseTypeCourseMapper.selectList(new QueryWrapper<CourseTypeCourseDO>().eq("course_id", courseDO.getId())).stream().map(CourseTypeCourseDO::getTypeId).toList();
            List<CourseTypeDO> typeList = courseTypeIds.stream().map(courseTypeId -> courseTypeMapper.selectById(courseTypeId)).toList();
            List<CourseType> TPList = typeList.stream().map(courseTypeDO -> courseConvertor.toCourseType(courseDO.getId(),courseTypeDO)).toList();
            //统计评教数
            Long courInfId = evaTaskMapper.selectCount(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courseDO.getId()));
            //添加到selfteachCourseCO中
            selfTeachCourseCO.setId(courseDO.getId());
            selfTeachCourseCO.setName(subjectDO.getName());
            selfTeachCourseCO.setTypeList(TPList);
            selfTeachCourseCO.setNature(subjectDO.getNature());
            selfTeachCourseCO.setEvaNum(courInfId.intValue());
            list.add(selfTeachCourseCO);

        }
        return list;
    }

//TODO(有问题待修改)
    @Override
    public List<CourseEntity> getSelfCourse(Integer semId) {
        //根据semId来找课程实体
        List<CourseDO> semesterId = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semId));
        //得到CourseEntity集合
        List<CourseEntity> courseEntities = semesterId.stream().map(courseDO -> toCourseEntity(courseDO.getId(), semId)).toList();



        return courseEntities;
    }
    @Override
    public List<SingleCourseEntity> getSelfCourseTime(String userName, Integer id) {
        //先根据课程id来查出课程实体
        CourseDO courseDO = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", id));
        //先得到CourseEntity
        CourseEntity courseEntity = toCourseEntity(id,courseDO.getSemesterId());
        //根据id来查出CourInfoDo集合
        List<CourInfDO> courInfDOS = courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id", id));
        //转化成SingleCourseEntity
        List<SingleCourseEntity> list = courInfDOS.stream().map(courInfDO -> courseConvertor.toSingleCourseEntity(courseEntity, courInfDO)).toList();
        return list;
    }

    private SemesterEntity getSemester(Integer semId){
        SemesterDO semesterDO=null;
        if(semId!=null){
            semesterDO= semesterMapper.selectById(semId);
            return courseConvertor.toSemesterEntity(semesterDO);
        }
        return null;
    }
    private List<CoursePeriod> toGetCourseTime( Integer id){
        QueryWrapper<CourInfDO> queryWrapper = Wrappers.query();
        queryWrapper.eq("course_id", id);
        // 执行查询
        List<CourInfDO> resultList = courInfMapper.selectList(queryWrapper);

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
            }


        }
        return finalResult;

    }
    private CourInfDO getCourseInfo( CourseDO course){

        return  courInfMapper.selectById(course.getId());
    }
    private CourseEntity toCourseEntity(Integer courseId,Integer semId){
        //构造semester
        SemesterEntity semesterEntity = getSemester(semId);
        //构造courseDo
        CourseDO courseDO = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", courseId).eq("semester_id", semId));
        //构造subject
        SubjectEntity subjectEntity = courseConvertor.toSubjectEntity(subjectMapper.selectById(courseDO.getSubjectId()));
        //构造userEntity
        UserEntity userEntity =toUserEntity(courseMapper.selectById(courseId).getTeacherId());
        return courseConvertor.toCourseEntity(courseDO,subjectEntity,userEntity,semesterEntity);
    }
    private UserEntity toUserEntity(Integer userId){
        //得到uer对象
        SysUserDO userDO = userMapper.selectById(userId);
        //根据userId找到角色id集合
        List<Integer> roleIds = userRoleMapper.selectList(new QueryWrapper<SysUserRoleDO>().eq("user_id", userId)).stream().map(SysUserRoleDO::getRoleId).toList();
        //根据角色id集合找到角色对象集合
        List<RoleEntity> roleEntities = roleMapper.selectList(new QueryWrapper<SysRoleDO>().in("id", roleIds)).stream().map(roleDO -> roleConverter.toRoleEntity(roleDO)).toList();
        //根据角色id集合找到角色菜单表中的菜单id集合
        List<Integer> menuIds = roleMenuMapper.selectList(new QueryWrapper<SysRoleMenuDO>().in("role_id", roleIds)).stream().map(SysRoleMenuDO::getMenuId).toList();
        //根据menuids找到菜单对象集合
//        List<MenuEntity> menuEntities = menuMapper.selectList(new QueryWrapper<SysMenuDO>().in("id", menuIds)).stream().map(menuDO -> menuConvertor.toMenuEntity(menuDO)).toList();


        return userConverter.toUserEntity(userDO,roleEntities);
    }
    private LocalDateTime toLocalDateTime(String s){

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(s, formatter);
        LocalDateTime localDateTime = localDate.atStartOfDay();
        return localDateTime;
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
            wrapper.between("start_day",courseQuery.getStartDay(),courseQuery.getEndDay());
        }else if(courseQuery.getStartDay()!=null&&courseQuery.getEndDay()==null){
            wrapper.ge("start_day",courseQuery.getStartDay());
        }else if(courseQuery.getStartDay()==null&&courseQuery.getEndDay()!=null){
            wrapper.le("end_day",courseQuery.getEndDay());
        }else{
            wrapper=null;
        }

    }


}
