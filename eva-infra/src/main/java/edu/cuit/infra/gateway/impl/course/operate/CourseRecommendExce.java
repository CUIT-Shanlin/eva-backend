package edu.cuit.infra.gateway.impl.course.operate;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.client.dto.clientobject.course.RecommendCourseCO;
import edu.cuit.client.dto.data.course.CourseTime;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.infra.convertor.course.CourseConvertor;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseTypeCourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.*;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.gateway.impl.course.CourseQueryGatewayImpl;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.description.type.TypeList;
import org.springframework.stereotype.Component;

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
    private final FormRecordMapper recordMapper;
    private final CourOneEvaTemplateMapper courOneEvaTemplateMapper;
    private final SysUserMapper userMapper;
    private final FormTemplateMapper formTemplateMapper;
    private final CourseQueryGatewayImpl courseQueryGateway;
    public List<CourseDO> RecommendCourse(Integer semId, String userName){
        //得到当前是第几周
        CourseTime courseTime = courseQueryGateway.toCourseTime(semId);
        //查询user
        SysUserDO user = userMapper.selectOne(new QueryWrapper<SysUserDO>().eq("name", userName));
        //查询user授课程集合
        List<CourseDO> courseDOS1 = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", user.getId()).eq("sem_id", semId));
        List<Integer> courseIds = courseDOS1.stream().map(CourseDO::getId).toList();
        //找出老师所要评教的课程
        List<EvaTaskDO> taskDOList = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id", user.getId()).eq("status",0));
        List<CourInfDO> evaCourInfo;
        Set<Integer> evaCourInfoSet=new HashSet<>();
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
            evaCourInfo = new ArrayList<>();
        }
        //包含了所有教学课程和评教课程
        evaCourInfoSet.addAll(courIdList);
        //找出评教次数大于等于8次的课程ID集合
        List<EvaTaskDO> taskList = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("status", 1).or().eq("status", 0));
        List<Integer> courInfoList = taskList.stream().map(EvaTaskDO::getCourInfId).toList();
        List<CourInfDO> courseDOS = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("id", courInfoList));
        Map<Integer, List<CourInfDO>> collect = courseDOS.stream().collect(Collectors.groupingBy(CourInfDO::getCourseId));
        List<Integer> collect1 = collect.entrySet().stream().filter(entry -> entry.getValue().size() >= 8).map(Map.Entry::getKey).toList();
        //从collet中找出小于8次的
        List<Integer> leList = collect.entrySet().stream().filter(entry -> entry.getValue().size() < 8).map(Map.Entry::getKey).toList();
        //包含了所有教学课程和评教课程和所有评教次数大于等于8次的课程ID集合
        evaCourInfoSet.addAll(collect1);
        List<CourseDO> courseList = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("sem_id", semId));
        //符合硬性要求的课程
        List<CourseDO> list = courseList.stream().filter(course -> !evaCourInfoSet.contains(course.getId())).toList();
        List<RecommendCourseCO> recommendCourse = getRecommendCourse(leList, list, courseDOS1, courseTime);

        return null;
    }

    private List<RecommendCourseCO> getRecommendCourse(List<Integer> leList,List<CourseDO> list, List<CourseDO> courseDOS1,CourseTime courseTime){
        //找出list中的id不在leList的集合
        List<CourseDO> notExistCourse = list.stream().filter(courseDO -> !leList.contains(courseDO.getId())).toList();
        List<CourseDO> existCourse = list.stream().filter(courseDO -> leList.contains(courseDO.getId())).toList();
        //notExistCourse中根据老师id分类
        Map<Integer, List<CourseDO>> map = notExistCourse.stream().collect(Collectors.groupingBy(CourseDO::getTeacherId));
        Map<Integer, List<CourseDO>> map = existCourse.stream().collect(Collectors.groupingBy(CourseDO::getTeacherId));
        List<RecommendCourseCO> recommendList=new ArrayList<>();
        for (Map.Entry<Integer, List<CourseDO>> entry : map.entrySet()) {
          if(entry.getValue().size()>=2){
              //该老师的课程还没有被评教过，优先级priority: 5
              recommendList.add(createRecommentList(entry.getValue(), 5, courseDOS1,courseTime));
          }else{
              //该老师课程被评教过，优先级priority: 2
              recommendList.add(createRecommentList(entry.getValue(), 2, courseDOS1,courseTime));
          }
        }


        return null;
    }
    private List<RecommendCourseCO> createRecommentList(List<CourseDO> list,Integer priority,List<CourseDO> slefCourseDo,CourseTime courseTime){
        List<RecommendCourseCO> recommendCourseCOS=new ArrayList<>();
        for (CourseDO courseDO : list) {
            SysUserDO userDO = userMapper.selectById(courseDO.getTeacherId);
            SubjectDO subjectDO = subjectMapper.selectById(courseDO.getSubjectId);
            Map<List<CourseType>, Double> course = getCourseTypeAndSimilarity(courseDO, slefCourseDo);
            List<CourInfDO> courInfo = courInfMapper.selectList(new QueryWrapper<CourInfDO>()
                    .eq("course_id", courseDO.getId())
                    .gt("week", courseTime.getWeek())
                    .or()
                    .eq("week", courseTime.getWeek())
                    .gt("day", courseTime.getDay()));
            for (CourInfDO courInfDO : courInfo) {
                RecommendCourseCO recommend=new RecommendCourseCO()
                        .setId(courInfDO.getId())
                        .setLocation(courInfDO.getLocation())
                        .setEvaNum(evaTaskMapper.selectCount(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courInfDO.getId()).eq("status", 1).or().eq("status", 0)));

            }

        }
    }
    private Map<List<CourseType>,Double> getCourseTypeAndSimilarity(CourseDo courseDo,List<CourseDo> slefCourseDo){
        Map<List<CourseType>,Double> map=new HashMap<>();
        List<CourseType> typeList = getCourseType(courseDo);
        //自己教学课程类型集合
        List<CourseType> selfList=new ArrayList<>();
        for (CourseDo aDo : slefCourseDo) {
             selfList.add(getCourseType(aDo));
        }
        //找出typeList与slefList的交集
        List<CourseType> sameList=new ArrayList<>();
        for (CourseType courseType : selfList) {
            if(typeList.stream().map(CourseType::getId).contains(courseType.getId())){
                sameList.add(courseType);
            }
        }
        Double socre=sameList.size()*1.0/(typeList.size()>selfList.size()?typeList.size():selfList.size());
        map.put(typeList,socre);
        return map;
    }
    private List<CourseType> getCourseType(CourseDO courseDo){
        List<CourseTypeCourseDO> courseId = courseTypeCourseMapper.selectList(new QueryWrapper<CourseTypeCourseDO>().eq("course_id", courseDO.getId()));
        List<CourseTypeDO> courseTypeList = courseId.stream().map(courseTypeCourseDO -> courseTypeMapper.selectById(courseTypeCourseDO.getTypeId())).toList();
        return courseTypeList.stream().map(courseTypeDO -> courseConvertor.toCourseType(courseDo.getId(), courseTypeDO)).toList();
    }

}
