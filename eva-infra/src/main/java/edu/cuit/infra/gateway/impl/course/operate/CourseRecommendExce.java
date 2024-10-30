package edu.cuit.infra.gateway.impl.course.operate;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.client.dto.clientobject.course.RecommendCourseCO;
import edu.cuit.client.dto.data.course.CourseTime;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.client.dto.query.CourseQuery;
import edu.cuit.client.dto.query.condition.MobileCourseQuery;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.infra.convertor.course.CourseConvertor;
import edu.cuit.infra.dal.database.dataobject.course.*;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.*;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.gateway.impl.course.CourseQueryGatewayImpl;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.description.type.TypeList;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class CourseRecommendExce {
    private final CourseConvertor courseConvertor;
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final CourseTypeCourseMapper courseTypeCourseMapper;
    private final CourseTypeMapper courseTypeMapper;
    private final SubjectMapper subjectMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final SysUserMapper userMapper;
    private final SemesterMapper semesterMapper;


    public List<RecommendCourseCO> RecommendCourse(Integer semId, String userName,CourseTime courseTime){
//        userName="ganjianhong";
        //查询user
        SysUserDO user = userMapper.selectOne(new QueryWrapper<SysUserDO>().eq("username", userName));
        if(user==null)throw new QueryException("用户不存在");
        //查询user授课程集合
        List<CourseDO> courseDOS1 = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", user.getId()).eq("semester_id", semId));
        List<Integer> courseIds = courseDOS1.stream().map(CourseDO::getId).toList();
        //找出老师所要评教的课程
        List<EvaTaskDO> taskDOList = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id", user.getId()).eq("status",0));
        List<CourInfDO> evaCourInfo;
        Set<Integer> evaCourInfoSet;
        if(!taskDOList.isEmpty()) {
            List<Integer> evaCourInfoList = taskDOList.stream().map(EvaTaskDO::getCourInfId).toList();
            evaCourInfo = courInfMapper.selectList( new QueryWrapper<CourInfDO>()
                    .in(!evaCourInfoList.isEmpty(),"id", evaCourInfoList)
                    .and(wrapper -> wrapper
                            .gt("week", courseTime.getWeek())
                            .or()
                            .eq("week", courseTime.getWeek())
                            .gt("day", courseTime.getDay())
                    ));
            //TODO: 2022/5/26 待优化当天的课程
            //得到待评教的courseId集合（set集合）
            evaCourInfoSet = evaCourInfo.stream().map(CourInfDO::getCourseId).collect(Collectors.toSet());
        } else {
            evaCourInfoSet = new HashSet<>();
        }
        //包含了所有教学课程和评教课程
        evaCourInfoSet.addAll(courseIds);
        //找出评教次数大于等于8次的课程ID集合
        List<EvaTaskDO> taskList = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("status", 1).or().eq("status", 0));
        List<Integer> courInfoList = taskList.stream().map(EvaTaskDO::getCourInfId).toList();
        List<CourInfDO> courseDOS=new ArrayList<>();
        if(!courInfoList.isEmpty())courseDOS = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in(true,"id", courInfoList));
        Map<Integer, List<CourInfDO>> collect = courseDOS.stream().collect(Collectors.groupingBy(CourInfDO::getCourseId));
        List<Integer> collect1 = collect.entrySet().stream().filter(entry -> entry.getValue().size() >= 8).map(Map.Entry::getKey).toList();
        //从collet中找出小于8次的
        List<Integer> leList = collect.entrySet().stream().filter(entry -> entry.getValue().size() < 8).map(Map.Entry::getKey).toList();
        //包含了所有教学课程和评教课程和所有评教次数大于等于8次的课程ID集合
        evaCourInfoSet.addAll(collect1);
        List<CourseDO> courseList = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semId));
        //符合硬性要求的课程
        List<CourseDO> list = courseList.stream().filter(course -> !evaCourInfoSet.contains(course.getId())).toList();
        List<RecommendCourseCO> recommendCourse = getRecommendCourse(leList, list, courseDOS1, courseTime);
        //根据recommendCourse中的proriority进行排序(降序)，如果优先级相同，那么根据课程时间来进行排序(升序)
        Stream<RecommendCourseCO> stream = recommendCourse.stream();
        // 按照 prioty属性进行降序排序
        Comparator<RecommendCourseCO> priotyComparator = Comparator.comparing(RecommendCourseCO::getPriority);
        // 如果 prioty 相同，则按 time 的 week和day 属性进行升序排序
        Comparator<RecommendCourseCO> weekComparator = Comparator.comparing(RecommendCourseCO::getTime, Comparator.comparing(CourseTime::getWeek)).reversed();
        Comparator<RecommendCourseCO> dayComparator = Comparator.comparing(RecommendCourseCO::getTime, Comparator.comparing(CourseTime::getDay)).reversed();

        // 组合比较器
        Comparator<RecommendCourseCO> combinedComparator = priotyComparator.thenComparing(weekComparator);
        Comparator<RecommendCourseCO> recommendCourseCOComparator = combinedComparator.thenComparing(dayComparator);
        // 排序
        Stream<RecommendCourseCO> sortedStream = stream.sorted(recommendCourseCOComparator);
        List<RecommendCourseCO> result = sortedStream.toList();
        //过滤出前三周的，如果不够25条就往后延迟一周，直到有25条记录，如果都小于25就返回当前的就行
        List<RecommendCourseCO> reCourseList = result.stream().filter(recommendCourseCO -> recommendCourseCO.getTime().getWeek() <= courseTime.getWeek() + 3).toList();
        int i=4;
       while (reCourseList.size()<5){
           int finalI = i;
           reCourseList = result.stream().filter(recommendCourseCO -> recommendCourseCO.getTime().getWeek() <= courseTime.getWeek() + finalI).toList();
           i++;
           if(reCourseList.size()==result.size()){
               break;
           }
       }
        //如果result长度大于25，则返回前25个，反之全部返回
        return reCourseList.size()>25?reCourseList.subList(0,25):reCourseList;
    }

    private List<RecommendCourseCO> getRecommendCourse(List<Integer> leList,List<CourseDO> list, List<CourseDO> courseDOS1,CourseTime courseTime){
        //找出list中的id不在leList的集合
        List<CourseDO> notExistCourse = list.stream().filter(courseDO -> !leList.contains(courseDO.getId())).toList();
        List<CourseDO> existCourse = list.stream().filter(courseDO -> leList.contains(courseDO.getId())).toList();
        //notExistCourse中根据老师id分类
        Map<Integer, List<CourseDO>> map = notExistCourse.stream().collect(Collectors.groupingBy(CourseDO::getTeacherId));
//        Map<Integer, List<CourseDO>> map2 = existCourse.stream().collect(Collectors.groupingBy(CourseDO::getTeacherId));
        List<RecommendCourseCO> recommendList=new ArrayList<>();
        for (Map.Entry<Integer, List<CourseDO>> entry : map.entrySet()) {
          if(entry.getValue().size()>=2){
              //该老师的课程还没有被评教过，优先级priority: 5
              recommendList.addAll(createRecommentList(entry.getValue(), 5, courseDOS1,courseTime));
          }else{
              //该老师课程被评教过，优先级priority: 2
              recommendList.addAll(createRecommentList(entry.getValue(), 2, courseDOS1,courseTime));
          }
            }
        //对已经评教过的existCourse，根据课程老师id进行分类
        Map<Integer, List<CourseDO>> collect = existCourse.stream().collect(Collectors.groupingBy(CourseDO::getTeacherId));
        for (Map.Entry<Integer, List<CourseDO>> exist : collect.entrySet()) {
            recommendList.addAll(createRecommentList(exist.getValue(), -2, courseDOS1, courseTime));
        }
      return recommendList;
    }
    private List<RecommendCourseCO> createRecommentList(List<CourseDO> list,Integer priority,List<CourseDO> slefCourseDo,CourseTime courseTime){
        SysUserDO userDO = userMapper.selectById(list.get(0).getTeacherId());
        int evaTeacherNum=0;
        List<RecommendCourseCO> recommendCourseCOS=new ArrayList<>();
        for (CourseDO courseDO : list) {
            SubjectDO subjectDO = subjectMapper.selectById(courseDO.getSubjectId());
            Map<List<CourseType>, Double> course = getCourseTypeAndSimilarity(courseDO,slefCourseDo);
            List<CourInfDO> courInfo = courInfMapper.selectList( new QueryWrapper<CourInfDO>()
                    .eq("course_id", courseDO.getId())
                    .and(wrapper ->
                            wrapper.gt("week", courseTime.getWeek())
                                    .or()
                                    .allEq(Map.of("week", courseTime.getWeek(), "day", courseTime.getDay()))
                    )
            );
            for (CourInfDO courInfDO : courInfo) {
                RecommendCourseCO recommend=new RecommendCourseCO()
                        .setId(courInfDO.getId())
                        .setLocation(courInfDO.getLocation())
                        .setName(subjectDO.getName())
                        .setNature(subjectDO.getNature())
                        .setTeacherName(userDO.getName());
                for (Map.Entry<List<CourseType>, Double> listDoubleEntry : course.entrySet()) {
                    recommend.setTypeList(listDoubleEntry.getKey());
                    recommend.setPriority(priority+listDoubleEntry.getValue()*4);
                    recommend.setTypeSimilarity(listDoubleEntry.getValue());
                }
                CourseTime courestime = courseConvertor.toCourseTime(courInfDO);
                recommend.setTime(courestime);
                Long num = evaTaskMapper.selectCount( new QueryWrapper<EvaTaskDO>()
                        .eq("cour_inf_id", courInfDO.getId())
                        .and(wrapper -> wrapper.eq("status", 1).or().eq("status", 0)));
                recommend.setEvaNum(Math.toIntExact(num));
                evaTeacherNum+=Math.toIntExact(num);
                recommendCourseCOS.add(recommend);
            }

        }
        int finalEvaTeacherNum = evaTeacherNum;
        recommendCourseCOS.forEach(recommendCourseCO -> recommendCourseCO.setEvaTeacherNum(finalEvaTeacherNum));
        return recommendCourseCOS;
    }
    private Map<List<CourseType>,Double> getCourseTypeAndSimilarity(CourseDO courseDo,List<CourseDO> slefCourseDo){
        Map<List<CourseType>,Double> map=new HashMap<>();
        List<CourseType> typeList = getCourseType(courseDo);
        //自己教学课程类型集合
        List<CourseType> selfList=new ArrayList<>();
        for (CourseDO aDo : slefCourseDo) {
             selfList.addAll(getCourseType(aDo));
        }
        //找出typeList与slefList的交集
        List<CourseType> sameList=new ArrayList<>();
        for (CourseType courseType : selfList) {
            if(typeList.stream().map(CourseType::getId).toList().contains(courseType.getId())){
                sameList.add(courseType);
            }
        }
        Double socre=sameList.size()*1.0/(Math.max(typeList.size(), selfList.size()));
        map.put(typeList,socre);
        return map;
    }
    private List<CourseType> getCourseType(CourseDO courseDo){
        List<CourseTypeCourseDO> courseId = courseTypeCourseMapper.selectList(new QueryWrapper<CourseTypeCourseDO>().eq("course_id", courseDo.getId()));
        List<CourseTypeDO> courseTypeList = courseId.stream().map(courseTypeCourseDO -> courseTypeMapper.selectById(courseTypeCourseDO.getTypeId())).toList();
        return courseTypeList.stream().map(courseConvertor::toCourseType).toList();
    }
    public List<RecommendCourseCO> togetPeriodCourse(Integer semId, MobileCourseQuery courseQuery, String userName){
//        userName="ganjianhong";
        SemesterDO semesterDO = semesterMapper.selectById(semId);
        //老师教学课程
        SysUserDO user = userMapper.selectOne(new QueryWrapper<SysUserDO>().eq("username", userName));
        if(user==null)throw new QueryException("用户不存在");
        List<CourseDO> userCourse = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", user.getId()).eq("semester_id", semId));
        List<CourseDO> courseList=judeTimetoGetCourse(semesterDO, courseQuery);
        //根据老师id进行分类
        Map<Integer, List<CourseDO>> collect = courseList.stream().collect(Collectors.groupingBy(CourseDO::getTeacherId));
        List<RecommendCourseCO> result=new ArrayList<>();
        if(courseQuery.getStartDay()!=null&&courseQuery.getEndDay()!=null){
            CourseTime startTime = toGetCourseTime(semesterDO.getStartDate(), togetLocalDate(courseQuery.getStartDay()));
            CourseTime endTime = toGetCourseTime(semesterDO.getStartDate(), togetLocalDate(courseQuery.getEndDay()));
            for (Map.Entry<Integer, List<CourseDO>> map : collect.entrySet()) {
                List<RecommendCourseCO> recommentListEndandStart = createRecommentListEndandStart(map.getValue(), 0, userCourse, startTime, endTime);
                result.addAll(recommentListEndandStart);
            }
//            return createRecommentListEndandStart(courseList, 0, userCourse, startTime, endTime);
        } else if(courseQuery.getStartDay() != null&& courseQuery.getEndDay()==null){
            CourseTime startTime = toGetCourseTime(semesterDO.getStartDate(), togetLocalDate(courseQuery.getStartDay()));
            for (Map.Entry<Integer, List<CourseDO>> integerListEntry : collect.entrySet()) {
                List<RecommendCourseCO> recommentList = createRecommentList(integerListEntry.getValue(), 0, userCourse, startTime);
                result.addAll(recommentList);
            }
//            return createRecommentList(courseList, 0, userCourse, startTime);

        } else if(courseQuery.getEndDay() != null&& courseQuery.getStartDay()==null){
            CourseTime endTime = toGetCourseTime(semesterDO.getStartDate(), togetLocalDate(courseQuery.getEndDay()));
            for (Map.Entry<Integer, List<CourseDO>> integerListEntry : collect.entrySet()) {
                List<RecommendCourseCO> recommentListEnd = createRecommentListEnd(integerListEntry.getValue(), 0, userCourse, endTime);
                result.addAll(recommentListEnd);
            }
//            return createRecommentListEnd(courseList, 0, userCourse, endTime);
        }

        return result;
    }

    private List<RecommendCourseCO> createRecommentListEndandStart(List<CourseDO> list,Integer priority,List<CourseDO> slefCourseDo,CourseTime startTime,CourseTime endTime){
        SysUserDO userDO = userMapper.selectById(list.get(0).getTeacherId());
        int evaTeacherNum=0;
        List<RecommendCourseCO> recommendCourseCOS=new ArrayList<>();
        for (CourseDO courseDO : list) {
            Integer id=courseDO.getId();
            SubjectDO subjectDO = subjectMapper.selectById(courseDO.getSubjectId());
            Map<List<CourseType>, Double> course = getCourseTypeAndSimilarity(courseDO,slefCourseDo);
            QueryWrapper<CourInfDO> courseInfQueryWrapper = new QueryWrapper<>();
//            courseInfQueryWrapper.eq("course_id", courseDO.getId());
            if (Objects.equals(startTime.getWeek(), endTime.getWeek())) {
                // 当周数相同时，直接比较星期几
                courseInfQueryWrapper
                        .eq("course_id", courseDO.getId())
                        .ge("day", startTime.getDay())
                        .le("day", endTime.getDay());
            } else {
                // 当周数不同时，分开处理
                courseInfQueryWrapper
                        .and(wrapper -> wrapper
                                .eq("course_id", courseDO.getId())
                                .eq("week", startTime.getWeek())
                                .ge("day", startTime.getDay()))
                        .or(wrapper -> wrapper
                                .eq("course_id", courseDO.getId())
                                .eq("week", endTime.getWeek())
                                .le("day", endTime.getDay()))
                        .or(wrapper -> wrapper
                                .eq("course_id", courseDO.getId())
                                .gt("week", startTime.getWeek())
                                .lt("week", endTime.getWeek()));

            }

            List<CourInfDO> courInfo = courInfMapper.selectList(courseInfQueryWrapper);
            for (CourInfDO courInfDO : courInfo) {
                RecommendCourseCO recommend=new RecommendCourseCO()
                        .setId(courInfDO.getId())
                        .setLocation(courInfDO.getLocation())
                        .setName(subjectDO.getName())
                        .setNature(subjectDO.getNature())
                        .setTeacherName(userDO.getName());
                for (Map.Entry<List<CourseType>, Double> listDoubleEntry : course.entrySet()) {
                    recommend.setTypeList(listDoubleEntry.getKey());
                    recommend.setPriority(priority+listDoubleEntry.getValue()*4);
                    recommend.setTypeSimilarity(listDoubleEntry.getValue());
                }
                CourseTime courestime = courseConvertor.toCourseTime(courInfDO);
                recommend.setTime(courestime);
                Long num = evaTaskMapper.selectCount( new QueryWrapper<EvaTaskDO>()
                        .eq("cour_inf_id", courInfDO.getId())
                        .and(wrapper -> wrapper.eq("status", 1).or().eq("status", 0)));
                recommend.setEvaNum(Math.toIntExact(num));
                evaTeacherNum+=Math.toIntExact(num);
                recommendCourseCOS.add(recommend);
            }

        }
        int finalEvaTeacherNum = evaTeacherNum;
        recommendCourseCOS.forEach(recommendCourseCO -> recommendCourseCO.setEvaTeacherNum(finalEvaTeacherNum));
        return recommendCourseCOS;
    }

    private List<RecommendCourseCO> createRecommentListEnd(List<CourseDO> list,Integer priority,List<CourseDO> slefCourseDo,CourseTime courseTime){
        SysUserDO userDO = userMapper.selectById(list.get(0).getTeacherId());
        int evaTeacherNum=0;
        List<RecommendCourseCO> recommendCourseCOS=new ArrayList<>();
        for (CourseDO courseDO : list) {
            SubjectDO subjectDO = subjectMapper.selectById(courseDO.getSubjectId());
            Map<List<CourseType>, Double> course = getCourseTypeAndSimilarity(courseDO,slefCourseDo);
            List<CourInfDO> courInfo = courInfMapper.selectList( new QueryWrapper<CourInfDO>()
                    .eq("course_id", courseDO.getId())
                    .and(wrapper ->
                            wrapper.lt("week", courseTime.getWeek())
                                    .or()
                                    .eq("week", courseTime.getWeek())
                                    .lt("day", courseTime.getDay())
                    )
            );
            for (CourInfDO courInfDO : courInfo) {
                RecommendCourseCO recommend=new RecommendCourseCO()
                        .setId(courInfDO.getId())
                        .setLocation(courInfDO.getLocation())
                        .setName(subjectDO.getName())
                        .setNature(subjectDO.getNature())
                        .setTeacherName(userDO.getName());
                for (Map.Entry<List<CourseType>, Double> listDoubleEntry : course.entrySet()) {
                    recommend.setTypeList(listDoubleEntry.getKey());
                    recommend.setPriority(priority+listDoubleEntry.getValue()*4);
                    recommend.setTypeSimilarity(listDoubleEntry.getValue());
                }
                CourseTime courestime = courseConvertor.toCourseTime(courInfDO);
                recommend.setTime(courestime);
                Long num = evaTaskMapper.selectCount( new QueryWrapper<EvaTaskDO>()
                        .eq("cour_inf_id", courInfDO.getId())
                        .and(wrapper -> wrapper.eq("status", 1).or().eq("status", 0)));
                recommend.setEvaNum(Math.toIntExact(num));
                evaTeacherNum+=Math.toIntExact(num);
                recommendCourseCOS.add(recommend);
            }

        }
        int finalEvaTeacherNum = evaTeacherNum;
        recommendCourseCOS.forEach(recommendCourseCO -> recommendCourseCO.setEvaTeacherNum(finalEvaTeacherNum));
        return recommendCourseCOS;
    }
    private List<CourseDO> judeTimetoGetCourse(SemesterDO semesterDO, MobileCourseQuery courseQuery) {

        QueryWrapper<CourInfDO> courseInfQueryWrapper = new QueryWrapper<>();
        if(semesterDO==null)throw new QueryException("学期不存在");
        toJudgeTime(semesterDO,courseQuery,courseInfQueryWrapper);
        //课程时间
        List<CourInfDO> courInfDOS = courInfMapper.selectList(courseInfQueryWrapper);
        //得到courinfDOs中的courseId并去重
        List<Integer> courseDo1 = courInfDOS.stream().map(CourInfDO::getCourseId).distinct().toList();
        //
        List<List<Integer>> list=new ArrayList<>();
//        list.add(courseDo1);
        //如果课程名称不为null

        if(courseQuery.getKeyword()!=null&&!courseQuery.getKeyword().isEmpty()){
            List<CourseDO> listCourseDo=new ArrayList<>();
            List<SubjectDO> subjectDO = subjectMapper.selectList(new QueryWrapper<SubjectDO>().like("name", courseQuery.getKeyword()));
            if(subjectDO.isEmpty())throw new QueryException("没有对应科目的课程");
            listCourseDo=courseMapper.selectList(new QueryWrapper<CourseDO>().in("subject_id", subjectDO.stream().map(SubjectDO::getId).toList()).eq("semester_id",semesterDO.getId()));
            List<Integer> courseDo3 = listCourseDo.stream().map(CourseDO::getId).toList();
             list.add(courseDo3);
        }

        //老师和院系
       List<CourseDO> teacherCourseDolist=judeTeacherandDepartment( semesterDO.getId(),  courseQuery);
        if(teacherCourseDolist!=null){
            List<Integer> courseDo2 = teacherCourseDolist.stream().map(CourseDO::getId).toList();
             list.add(courseDo2);
        }
        //课程类型

        if(courseQuery.getTypeId()!=null&&courseQuery.getTypeId()>=0){
            List<Integer> typeCourseList=new ArrayList<>();
            CourseTypeDO courseTypeDO = courseTypeMapper.selectById(courseQuery.getTypeId());
            if(courseTypeDO==null)throw new QueryException("该课程类型不存在");
            List<CourseTypeCourseDO> courseTypeCourseDOS = courseTypeCourseMapper.selectList(new QueryWrapper<CourseTypeCourseDO>().eq("type_id", courseTypeDO.getId()));
            typeCourseList=courseTypeCourseDOS.stream().map(CourseTypeCourseDO::getCourseId).toList();
             list.add(typeCourseList);
        }

        //基于courseDo1来求这几个集合的交集
        // 基于 courseDo1 求交集
        List<Integer> intersection = new ArrayList<>(courseDo1);
        for (List<Integer> sublist : list) {
            intersection.retainAll(sublist);
            if(intersection.isEmpty()){
                throw new QueryException("在该时段内没有符合条件的课程");
            }
        }
        //交集
//        List<Integer> courseList=getInnerList(list);
        return courseMapper.selectList(new QueryWrapper<CourseDO>().in("id", intersection));
    }

    private List<Integer> getInnerList(List<List<Integer>> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        // 过滤掉空集合
        List<List<Integer>> nonEmptySets = list.stream()
                .filter(list1 -> !list1.isEmpty())
                .toList();

        if (nonEmptySets.isEmpty()) {
            return Collections.emptyList();
        }

        // 初始化交集为第一个非空集合
        List<Integer> result = new ArrayList<>(nonEmptySets.get(0));

        // 逐步求交集
        for (int i = 1; i < nonEmptySets.size(); i++) {
            result.retainAll(nonEmptySets.get(i));
        }

        return result;
    }

    private List<CourseDO> judeTeacherandDepartment(Integer semId, MobileCourseQuery courseQuery) {
        List<CourseDO> list=new ArrayList<>();
        if(courseQuery.getTeacherId()!=null&&courseQuery.getTeacherId()>=0){
            List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", courseQuery.getTeacherId()).eq("semester_id", semId));
            if (courseDOS.isEmpty())throw new QueryException("该教师没有该学期的课程或者在该时间段类没有对应课程");
            list.addAll(courseDOS);
            return list;
        }
        if(courseQuery.getTeacherId()==null||courseQuery.getTeacherId()<0&&courseQuery.getDepartmentName()!=null&&!courseQuery.getDepartmentName().isEmpty()){
            List<SysUserDO> user = userMapper.selectList(new QueryWrapper<SysUserDO>().eq("department", courseQuery.getDepartmentName()));
            List<Integer> userIds = user.stream().map(SysUserDO::getId).toList();
            if(userIds.isEmpty())throw new QueryException("该院系没有老师");
            List<CourseDO> courseDOS =new ArrayList<>();
            courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().in(true, "teacher_id", userIds));
            if (courseDOS.isEmpty())throw new QueryException("该院系教师还没有分配对应时间段课程");
            list.addAll(courseDOS);
            return list;
        }
        return null;

    }

    private void toJudgeTime(SemesterDO semesterDO, MobileCourseQuery courseQuery, QueryWrapper<CourInfDO> courseInfQueryWrapper) {
        if(courseQuery.getStartDay()!=null&&courseQuery.getEndDay()!=null){
            CourseTime startTime = toGetCourseTime(semesterDO.getStartDate(), togetLocalDate(courseQuery.getStartDay()));
            CourseTime endTime = toGetCourseTime(semesterDO.getStartDate(), togetLocalDate(courseQuery.getEndDay()));
            if (Objects.equals(startTime.getWeek(), endTime.getWeek())) {
                // 当周数相同时，直接比较星期几
                courseInfQueryWrapper
                        .ge("day", startTime.getDay())
                        .le("day", endTime.getDay());
            } else {
                // 当周数不同时，分开处理
                courseInfQueryWrapper
                        .and(wrapper -> wrapper
                                .eq("week", startTime.getWeek())
                                .ge("day", startTime.getDay()))
                        .or(wrapper -> wrapper
                                .eq("week", endTime.getWeek())
                                .le("day", endTime.getDay()))
                        .or(wrapper -> wrapper
                                .gt("week", startTime.getWeek())
                                .lt("week", endTime.getWeek()));

            }
        } else if(courseQuery.getStartDay() != null&&courseQuery.getEndDay() == null){
            CourseTime startTime = toGetCourseTime(semesterDO.getStartDate(), togetLocalDate(courseQuery.getStartDay()));
            courseInfQueryWrapper
                    .eq("week", startTime.getWeek())
                    .ge("day", startTime.getDay())
                    .or()
                    .gt("week", startTime.getWeek());
        }
        else if(courseQuery.getEndDay() != null&&courseQuery.getStartDay()==null){
            CourseTime endTime = toGetCourseTime(semesterDO.getStartDate(), togetLocalDate(courseQuery.getEndDay()));
            courseInfQueryWrapper
                    .eq("week", endTime.getWeek())
                    .le("day", endTime.getDay())
                    .or()
                    .lt("week", endTime.getWeek());
        }else {
            throw new UpdateException("指定的时间段不能都为空");
        }
    }

    private LocalDate togetLocalDate(String date){
        if(date==null){
            throw  new QueryException("这个时间段不能为空");
        }
        return LocalDate.parse(date);
    }
    private CourseTime toGetCourseTime(LocalDate startDate, LocalDate inStartDate){
        //计算inStartDate与startDate之间的天数差，来计算第几周的星期几
        long diff = ChronoUnit.DAYS.between(startDate, inStartDate);
        int currentWeek = (int) (diff / 7) + 1;
        int currentDayOfWeek = inStartDate.getDayOfWeek().getValue();
        return new CourseTime().setWeek(currentWeek).setDay(currentDayOfWeek);
    }

}
