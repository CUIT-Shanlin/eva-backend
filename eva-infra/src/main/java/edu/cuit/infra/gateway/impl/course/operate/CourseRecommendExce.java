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
import java.time.LocalDateTime;
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
        List<Integer> list1=new ArrayList<>();
        if(!courseDOS1.isEmpty())
         list1= courInfMapper.selectList(new QueryWrapper<CourInfDO>().in(true, "course_id", courseDOS1.stream().map(CourseDO::getId).toList())).stream().map(CourInfDO::getId).toList();
        List<Integer> courseIds = courseDOS1.stream().map(CourseDO::getId).toList();
        //找出老师所要评教的课程
        List<EvaTaskDO> taskDOList = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id", user.getId()).and(wrapper -> wrapper/*.eq("status", 1).or()*/.eq("status",0)));
        List<CourInfDO> evaCourInfo;
        Set<Integer> evaCourInfoSet;
        if(!taskDOList.isEmpty()) {
            List<Integer> evaCourInfoList = taskDOList.stream().map(EvaTaskDO::getCourInfId).toList();
            evaCourInfo = courInfMapper.selectList( new QueryWrapper<CourInfDO>()
                    .in(!evaCourInfoList.isEmpty(),"id", evaCourInfoList));
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
        List<Integer> tmpList = list1;
        if(!list1.isEmpty()){
            courInfoList= courInfoList.stream().filter(i -> !tmpList.contains(i)).toList();
        }
        List<CourInfDO> courseDOS=new ArrayList<>();
        if(!courInfoList.isEmpty()){
            for (Integer i : courInfoList) {
                courseDOS.add(courInfMapper.selectById(i));
            }
        }
        Map<Integer, List<CourInfDO>> collect=new HashMap<>();
//        Map<Integer, List<CourInfDO>> collect = courseDOS.stream().collect(Collectors.groupingBy(CourInfDO::getCourseId));
        for (CourInfDO courseDO : courseDOS) {
            collect.putIfAbsent(courseDO.getCourseId(),new ArrayList<>());
            collect.get(courseDO.getCourseId()).add(courseDO);
        }
        Map<Integer,Map<Integer,List<CourInfDO>>> teacherMap= new HashMap<>();
        for (Map.Entry<Integer, List<CourInfDO>> entry : collect.entrySet()) {
            Integer teacherId = courseMapper.selectById(entry.getKey()).getTeacherId();
            if(!teacherMap.containsKey(teacherId)){
                Map<Integer, List<CourInfDO> > map = new HashMap<>();
                map.put(entry.getKey(), entry.getValue());
                teacherMap.put(teacherId,map);
            }else{
                teacherMap.get(teacherId).put(entry.getKey(), entry.getValue());
            }
        }
        List<Integer> collect1=new ArrayList<>();
        List<Integer> leList=new ArrayList<>();
        for (Map.Entry<Integer, Map<Integer, List<CourInfDO>>> integerMapEntry : teacherMap.entrySet()) {
            int sum=0;
            for (Map.Entry<Integer, List<CourInfDO>> entry : integerMapEntry.getValue().entrySet()) {
                for (CourInfDO courInfDO : entry.getValue()) sum++;
            }
            if(sum>=8){
                //从collet中找出大于8次的
                for (Map.Entry<Integer, List<CourInfDO>> entry : integerMapEntry.getValue().entrySet()) {
                    collect1.add(entry.getKey());
                }
            }else{
                //从collet中找出小于8次的
                for (Map.Entry<Integer, List<CourInfDO>> entry : integerMapEntry.getValue().entrySet()) {
                    leList.add(entry.getKey());
                }
            }
        }
        //包含了所有教学课程和评教课程和所有评教次数大于等于8次的课程ID集合
        evaCourInfoSet.addAll(collect1);
        List<CourseDO> courseList = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semId));
        //符合硬性要求的课程
        List<CourseDO> list = courseList.stream().filter(course -> !evaCourInfoSet.contains(course.getId())).toList();
        List<RecommendCourseCO> recommendCourse = getRecommendCourse(leList, list, courseDOS1, courseTime,semId,courseDOS);
        //根据recommendCourse中的proriority进行排序(降序)，如果优先级相同，那么根据课程时间来进行排序(升序)
        Stream<RecommendCourseCO> stream = recommendCourse.stream();
        // 按照 prioty属性进行降序排序
        Comparator<RecommendCourseCO> priotyComparator = Comparator.comparing(RecommendCourseCO::getPriority).reversed();
        // 如果 prioty 相同，则按 time 的 week和day 属性进行升序排序
        Comparator<RecommendCourseCO> weekComparator = Comparator.comparing(RecommendCourseCO::getTime, Comparator.comparing(CourseTime::getWeek));
        Comparator<RecommendCourseCO> dayComparator = Comparator.comparing(RecommendCourseCO::getTime, Comparator.comparing(CourseTime::getDay));
        Comparator<RecommendCourseCO> startComparator = Comparator.comparing(RecommendCourseCO::getTime, Comparator.comparing(CourseTime::getStartTime));

        // 组合比较器
        Comparator<RecommendCourseCO> combinedComparator = priotyComparator.thenComparing(weekComparator);
        Comparator<RecommendCourseCO> recommendCourseCOComparator = combinedComparator.thenComparing(dayComparator);
        Comparator<RecommendCourseCO> recommendResult = recommendCourseCOComparator.thenComparing(startComparator);
        // 排序
        Stream<RecommendCourseCO> sortedStream = stream.sorted(recommendResult);
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

    private List<RecommendCourseCO> getRecommendCourse(List<Integer> leList,List<CourseDO> list, List<CourseDO> courseDOS1,CourseTime courseTime,Integer semId,List<CourInfDO> courInfDOS){
        List<RecommendCourseCO> recommendList=new ArrayList<>();
        //找出list中的id不在leList的集合
        List<CourseDO> notExistCourse = list.stream().filter(courseDO -> !leList.contains(courseDO.getId())).toList();
        List<CourseDO> existCourse = list.stream().filter(courseDO -> leList.contains(courseDO.getId())).toList();
        //notExistCourse中根据老师id分类
        Map<Integer, List<CourseDO>> map = notExistCourse.stream().collect(Collectors.groupingBy(CourseDO::getTeacherId));
//        Map<Integer, List<CourseDO>> map2 = existCourse.stream().collect(Collectors.groupingBy(CourseDO::getTeacherId));

        for (Map.Entry<Integer, List<CourseDO>> entry : map.entrySet()) {
            int l = Math.toIntExact(courseMapper.selectCount(new QueryWrapper<CourseDO>().eq("teacher_id", entry.getKey()).eq("semester_id", semId)));
            if(entry.getValue().size()>=l){
              //该老师的课程还没有被评教过，优先级priority: 5
              recommendList.addAll(createRecommentList(entry.getValue(), 5, courseDOS1,courseTime,courInfDOS));
          }else{
              //该老师课程被评教过，优先级priority: 2
              recommendList.addAll(createRecommentList(entry.getValue(), 2, courseDOS1,courseTime,courInfDOS));
          }
            }
        //对已经评教过的existCourse，根据课程老师id进行分类
        Map<Integer, List<CourseDO>> collect = existCourse.stream().collect(Collectors.groupingBy(CourseDO::getTeacherId));
        for (Map.Entry<Integer, List<CourseDO>> exist : collect.entrySet()) {
            recommendList.addAll(createRecommentList(exist.getValue(), 0, courseDOS1, courseTime,courInfDOS));
        }


        return returnList(recommendList);
    }
    private List<RecommendCourseCO> returnList(List<RecommendCourseCO> recommendList){
        Map<String, List<RecommendCourseCO>> result = recommendList.stream().collect(Collectors.groupingBy(RecommendCourseCO::getTeacherName));
        List<RecommendCourseCO> returnList = new ArrayList<>();
        for (Map.Entry<String, List<RecommendCourseCO>> entry : result.entrySet()) {
            int evaTeacher=0;
            //根据课程名和nature进行分组
            Map<String, List<RecommendCourseCO>> collect2 = entry.getValue().stream().collect(Collectors.groupingBy(course->course.getName()+course.getNature()));
            for (Map.Entry<String, List<RecommendCourseCO>> entry2 : collect2.entrySet()) {
                evaTeacher+=entry2.getValue().get(0).getEvaNum();
            }
            for (RecommendCourseCO recommendCourseCO : entry.getValue()) {
                recommendCourseCO.setEvaTeacherNum(evaTeacher);
            }
            returnList.addAll(entry.getValue());
        }

        return returnList;
    }
    private List<RecommendCourseCO> createRecommentList(List<CourseDO> list,Integer priority,List<CourseDO> slefCourseDo,CourseTime courseTime,List<CourInfDO> courInfDOS){
        SysUserDO userDO = userMapper.selectById(list.get(0).getTeacherId());
//        int evaTeacherNum=0;
        List<RecommendCourseCO> recommendCourseCOS=new ArrayList<>();
        for (CourseDO courseDO : list) {
            int EvaNum=0;
            SubjectDO subjectDO = subjectMapper.selectById(courseDO.getSubjectId());
            Map<List<CourseType>, Double> course = getCourseTypeAndSimilarity(courseDO,slefCourseDo);
            List<CourInfDO> courInfo = courInfMapper.selectList( new QueryWrapper<CourInfDO>()
                    .eq("course_id", courseDO.getId())
                    .and(wrapper ->
                            wrapper.gt("week", courseTime.getWeek())
                                    .or()
                                    .eq("week",courseTime.getWeek())
                                    .gt("day",courseTime.getDay())
                                    .or()
                                    .eq("week",courseTime.getWeek())
                                    .eq("day",courseTime.getDay())
                                    .ge("start_time",courseTime.getStartTime())
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
                   if(courInfDOS.stream().map(CourInfDO::getId).toList().contains(courInfDO.getId())&&priority>-2){
                       recommend.setPriority(-2+listDoubleEntry.getValue()*4);
                   }else{
                       recommend.setPriority(priority+listDoubleEntry.getValue()*4);
                   }
                    recommend.setTypeSimilarity(listDoubleEntry.getValue());
                }
                CourseTime courestime = courseConvertor.toCourseTime(courInfDO);
                recommend.setTime(courestime);
               /* Long num = evaTaskMapper.selectCount( new QueryWrapper<EvaTaskDO>()
                        .eq("cour_inf_id", courInfDO.getId())
                        .and(wrapper -> wrapper.eq("status", 1).or().eq("status", 0)));
                EvaNum+=Math.toIntExact(num);*/
//                evaTeacherNum+=Math.toIntExact(num);
                recommendCourseCOS.add(recommend);
            }
            List<Integer> list1 = courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id", courseDO.getId())).stream().map(CourInfDO::getId).toList();
            if(!list1.isEmpty())
             EvaNum =Math.toIntExact(evaTaskMapper.selectCount( new QueryWrapper<EvaTaskDO>()
                    .in("cour_inf_id",list1)
                    .and(wrapper -> wrapper.eq("status", 1).or().eq("status", 0))));
            int finalEvaNum = EvaNum;
            recommendCourseCOS.forEach(recommendCourseCO -> {
                if(recommendCourseCO.getName().equals(subjectDO.getName())&&recommendCourseCO.getNature().equals(subjectDO.getNature())){
                    recommendCourseCO.setEvaNum(finalEvaNum);
                }
            });
        }
//        int finalEvaTeacherNum = evaTeacherNum;
        recommendCourseCOS.forEach(recommendCourseCO -> recommendCourseCO.setEvaTeacherNum(0));
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
            CourseTime startTime = toGetCourseTime(semesterDO.getStartDate(), courseQuery.getStartDay());
            CourseTime endTime = toGetCourseTime(semesterDO.getStartDate(), courseQuery.getEndDay());
            for (Map.Entry<Integer, List<CourseDO>> map : collect.entrySet()) {
                List<RecommendCourseCO> recommentListEndandStart = createRecommentListEndandStart(map.getValue(), 0, userCourse, startTime, endTime);
                result.addAll(recommentListEndandStart);
            }
//            return createRecommentListEndandStart(courseList, 0, userCourse, startTime, endTime);
        } else if(courseQuery.getStartDay() != null&& courseQuery.getEndDay()==null){
            CourseTime startTime = toGetCourseTime(semesterDO.getStartDate(), courseQuery.getStartDay());
            for (Map.Entry<Integer, List<CourseDO>> integerListEntry : collect.entrySet()) {
                List<RecommendCourseCO> recommentList = createRecommentList(integerListEntry.getValue(), 0, userCourse, startTime,new ArrayList<>());
                result.addAll(recommentList);
            }
//            return createRecommentList(courseList, 0, userCourse, startTime);

        } else if(courseQuery.getEndDay() != null&& courseQuery.getStartDay()==null){
            CourseTime endTime = toGetCourseTime(semesterDO.getStartDate(), courseQuery.getEndDay());
            for (Map.Entry<Integer, List<CourseDO>> integerListEntry : collect.entrySet()) {
                List<RecommendCourseCO> recommentListEnd = createRecommentListEnd(integerListEntry.getValue(), 0, userCourse, endTime);
                result.addAll(recommentListEnd);
            }
//            return createRecommentListEnd(courseList, 0, userCourse, endTime);
        }

        return returnList(result);
    }

    private List<RecommendCourseCO> createRecommentListEndandStart(List<CourseDO> list,Integer priority,List<CourseDO> slefCourseDo,CourseTime startTime,CourseTime endTime){
        SysUserDO userDO = userMapper.selectById(list.get(0).getTeacherId());
//        int evaTeacherNum=0;
        List<RecommendCourseCO> recommendCourseCOS=new ArrayList<>();
        for (CourseDO courseDO : list) {
            int EvaNum=0;
            Integer id=courseDO.getId();
            SubjectDO subjectDO = subjectMapper.selectById(courseDO.getSubjectId());
            Map<List<CourseType>, Double> course = getCourseTypeAndSimilarity(courseDO,slefCourseDo);
            QueryWrapper<CourInfDO> courseInfQueryWrapper = new QueryWrapper<>();
//            courseInfQueryWrapper.eq("course_id", courseDO.getId());
            if (startTime.getWeek().equals(endTime.getWeek())) {
                // 当周数相同时，直接比较星期几
                courseInfQueryWrapper
                        .eq("course_id", courseDO.getId())
                        .eq("week", startTime.getWeek())
                        .and(wrapper->wrapper
                                .eq("day",startTime.getDay())
                                .ge("start_time",startTime.getStartTime())
                                .or()
                                .eq("day",endTime.getDay())
                                .le("start_time",endTime.getStartTime())
                                .or()
                                .gt("day", startTime.getDay())
                                .lt("day", endTime.getDay()));
            } else {
                // 当周数不同时，分开处理
                courseInfQueryWrapper
                        .and(wrapper -> wrapper
                                .eq("course_id", courseDO.getId())
                                .eq("week", startTime.getWeek())
                                .eq("day", startTime.getDay())
                                .ge("start_time",startTime.getStartTime())
                                .or()
                                .eq("course_id", courseDO.getId())
                                .eq("week", startTime.getWeek())
                                .gt("day", startTime.getDay())
                        )
                        .or(wrapper -> wrapper
                                .eq("course_id", courseDO.getId())
                                .eq("week", endTime.getWeek())
                                .eq("day", endTime.getDay())
                                .le("start_time",endTime.getStartTime())
                                .or()
                                .eq("course_id", courseDO.getId())
                                .eq("week", endTime.getWeek())
                                .lt("day", endTime.getDay())
                        )
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
//                evaTeacherNum+=Math.toIntExact(num);
                recommendCourseCOS.add(recommend);
            }
            List<Integer> list1 = courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id", courseDO.getId())).stream().map(CourInfDO::getId).toList();
            if(!list1.isEmpty())
                EvaNum =Math.toIntExact(evaTaskMapper.selectCount( new QueryWrapper<EvaTaskDO>()
                        .in("cour_inf_id",list1)
                        .and(wrapper -> wrapper.eq("status", 1).or().eq("status", 0))));
            int finalEvaNum = EvaNum;
            recommendCourseCOS.forEach(recommendCourseCO -> {
                if(recommendCourseCO.getName().equals(subjectDO.getName())&&recommendCourseCO.getNature().equals(subjectDO.getNature())){
                    recommendCourseCO.setEvaNum(finalEvaNum);
                }
            });

        }
//        int finalEvaTeacherNum = evaTeacherNum;
        recommendCourseCOS.forEach(recommendCourseCO -> recommendCourseCO.setEvaTeacherNum(0));
        return recommendCourseCOS;
    }

    private List<RecommendCourseCO> createRecommentListEnd(List<CourseDO> list,Integer priority,List<CourseDO> slefCourseDo,CourseTime courseTime){
        SysUserDO userDO = userMapper.selectById(list.get(0).getTeacherId());
//        int evaTeacherNum=0;
        List<RecommendCourseCO> recommendCourseCOS=new ArrayList<>();
        for (CourseDO courseDO : list) {
            int EvaNum=0;
            SubjectDO subjectDO = subjectMapper.selectById(courseDO.getSubjectId());
            Map<List<CourseType>, Double> course = getCourseTypeAndSimilarity(courseDO,slefCourseDo);
            List<CourInfDO> courInfo = courInfMapper.selectList( new QueryWrapper<CourInfDO>()
                    .eq("course_id", courseDO.getId())
                    .and(wrapper ->
                            wrapper.lt("week", courseTime.getWeek())
                                    .or()
                                    .eq("week", courseTime.getWeek())
                                    .lt("day", courseTime.getDay())
                                    .or()
                                    .eq("week",courseTime.getWeek())
                                    .eq("day",courseTime.getDay())
                                    .le("start_time",courseTime.getStartTime())
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

//                evaTeacherNum+=Math.toIntExact(num);
                recommendCourseCOS.add(recommend);
            }
            List<Integer> list1 = courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id", courseDO.getId())).stream().map(CourInfDO::getId).toList();
            if(!list1.isEmpty())
                EvaNum =Math.toIntExact(evaTaskMapper.selectCount( new QueryWrapper<EvaTaskDO>()
                        .in("cour_inf_id",list1)
                        .and(wrapper -> wrapper.eq("status", 1).or().eq("status", 0))));
            int finalEvaNum = EvaNum;
            recommendCourseCOS.forEach(recommendCourseCO -> {
                if(recommendCourseCO.getName().equals(subjectDO.getName())&&recommendCourseCO.getNature().equals(subjectDO.getNature())){
                    recommendCourseCO.setEvaNum(finalEvaNum);
                }
            });

        }
//        int finalEvaTeacherNum = evaTeacherNum;
        recommendCourseCOS.forEach(recommendCourseCO -> recommendCourseCO.setEvaTeacherNum(0));
        return recommendCourseCOS;
    }
    private List<CourseDO> judeTimetoGetCourse(SemesterDO semesterDO, MobileCourseQuery courseQuery) {

        QueryWrapper<CourInfDO> courseInfQueryWrapper = new QueryWrapper<>();
        toJudgeTime(semesterDO,courseQuery,courseInfQueryWrapper);

        //课程时间
        List<CourInfDO> courInfDOS = courInfMapper.selectList(courseInfQueryWrapper);
        //得到courinfDOs中的courseId并去重
        List<Integer> courseDo1 = courInfDOS.stream().map(CourInfDO::getCourseId).distinct().toList();
        List<CourseDO> courseDOS;
        if(courseDo1.isEmpty())courseDOS=new ArrayList<>();
         else courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semesterDO.getId()).in("id", courseDo1));
        courseDo1=courseDOS.stream().map(CourseDO::getId).toList();
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
            List<Integer> list1 = courseTypeCourseDOS.stream().map(CourseTypeCourseDO::getCourseId).distinct().toList();
            typeCourseList = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semesterDO.getId()).in("id", list1)).stream().map(CourseDO::getId).toList();
             list.add(typeCourseList);
        }

        //基于courseDo1来求这几个集合的交集
        // 基于 courseDo1 求交集
        List<Integer> intersection = new ArrayList<>(courseDo1);
        for (List<Integer> sublist : list) {
            intersection.retainAll(sublist);
        }
        if(intersection.isEmpty()){
            return new ArrayList<>();
        }else{
            return courseMapper.selectList(new QueryWrapper<CourseDO>().in("id", intersection));
        }

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
            courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().in(true, "teacher_id", userIds).eq("semester_id", semId));
            if (courseDOS.isEmpty())throw new QueryException("该院系教师还没有分配对应时间段课程");
            list.addAll(courseDOS);
            return list;
        }
        return null;

    }

    private void toJudgeTime(SemesterDO semesterDO, MobileCourseQuery courseQuery, QueryWrapper<CourInfDO> courseInfQueryWrapper) {
        if(courseQuery.getStartDay()!=null&&courseQuery.getEndDay()!=null){
            CourseTime startTime = toGetCourseTime(semesterDO.getStartDate(), courseQuery.getStartDay());
            CourseTime endTime = toGetCourseTime(semesterDO.getStartDate(), courseQuery.getEndDay());
            if (startTime.getWeek().equals(endTime.getWeek())) {
                // 当周数相同时，直接比较星期几
                courseInfQueryWrapper
                        .eq("week",startTime.getWeek())
                        .eq("day",startTime.getDay())
                        .ge("start_time",startTime.getStartTime())
                        .and(wrapper->wrapper.eq("week",startTime.getWeek()).eq("day",endTime.getDay()).le("end_time",endTime.getStartTime()))
                        .or()
                        .eq("week", startTime.getWeek())
                        .gt("day",startTime.getDay())
                        .lt("day",endTime.getDay());


            } else {
                // 当周数不同时，分开处理
                courseInfQueryWrapper
                        .and(wrapper -> wrapper
                                .eq("week", startTime.getWeek())
                                .eq("day", startTime.getDay())
                                .ge("start_time", startTime.getStartTime()))
                        .or(wrapper->wrapper
                                .eq("week", startTime.getWeek())
                                .gt("day", startTime.getDay()))
                        .or(wrapper -> wrapper
                                .eq("week", endTime.getWeek())
                                .eq("day", endTime.getDay())
                                .le("start_time", endTime.getStartTime()))
                        .or(courInfDOQueryWrapper -> courInfDOQueryWrapper
                                .eq("week", endTime.getWeek())
                                .lt("day", endTime.getDay()))
                        .or(wrapper -> wrapper
                                .gt("week", startTime.getWeek())
                                .lt("week", endTime.getWeek()));

            }
        } else if(courseQuery.getStartDay() != null&&courseQuery.getEndDay() == null){
            CourseTime startTime = toGetCourseTime(semesterDO.getStartDate(), courseQuery.getStartDay());
            courseInfQueryWrapper
                    .eq("week", startTime.getWeek())
                    .eq("day", startTime.getDay())
                    .ge("start_time", startTime.getStartTime())
                    .or()
                    .eq("week", startTime.getWeek())
                    .gt("day", startTime.getDay())
                    .or()
                    .gt("week", startTime.getWeek());
        }
        else if(courseQuery.getEndDay() != null&&courseQuery.getStartDay()==null){
            CourseTime endTime = toGetCourseTime(semesterDO.getStartDate(), courseQuery.getEndDay());
            courseInfQueryWrapper
                    .eq("week", endTime.getWeek())
                    .eq("day", endTime.getDay())
                    .le("start_time", endTime.getStartTime())
                    .or()
                    .eq("week", endTime.getWeek())
                    .lt("day", endTime.getDay())
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
    private CourseTime toGetCourseTime(LocalDate startDate, LocalDateTime inStartDate){
        Integer start=toGetStartTime(inStartDate);
        //计算inStartDate与startDate之间的天数差，来计算第几周的星期几
        long diff = ChronoUnit.DAYS.between(startDate, inStartDate);
        int currentWeek = (int) (diff / 7) + 1;
        int currentDayOfWeek = inStartDate.getDayOfWeek().getValue();
        return new CourseTime().setWeek(currentWeek).setDay(currentDayOfWeek).setStartTime(start);
    }
    public int toGetStartTime(LocalDateTime inStartDate){
        int result=0;
        int hour = inStartDate.getHour();
        int minute = inStartDate.getMinute();
        if(hour<=8|(hour<=8&&minute<=20))result=1;
        else if(hour <= 10|(hour<=10&&minute<=20))result=3;
        else if(hour<=14)result=5;
        else if (hour<=16)result=7;
        else if(hour<19|(hour<=19&&minute<=30))result=9;
        else result=11;
        return result;
    }

}
