package edu.cuit.infra.gateway.impl.courseimpl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.eva.CourseScoreCO;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.client.dto.data.course.CoursePeriod;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.client.dto.query.CourseQuery;
import edu.cuit.client.dto.query.PagingQuery;

import edu.cuit.client.dto.query.condition.CourseConditionalQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.course.*;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.infra.convertor.course.CourseConvertor;
import edu.cuit.infra.dal.database.dataobject.course.*;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.*;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CourseQueryGatewayImpl implements CourseQueryGateway {
    private final CourseConvertor courseConvertor;
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
    @Override
    public List<CourseEntity> page(PagingQuery<CourseConditionalQuery> courseQuery, Integer semId) {
        //获取semesterEntity
        SemesterEntity semester = getSemester(semId);
        // 根据courseQuery中的departmentName和is_delete为1以及courseQuery中的页数和一页显示数到userMapper中找对应数量的对应用户id，
        List<Integer> userIds=null;
        if(courseQuery.getQueryObj().getDepartmentName()!=null){
            Page<SysUserDO> pageUser=new Page<>(courseQuery.getPage(),courseQuery.getSize());
            pageUser=userMapper.selectPage(pageUser,new QueryWrapper<SysUserDO>().like("department",courseQuery.getQueryObj().getDepartmentName()));
            userIds=pageUser.getRecords().stream().map(SysUserDO::getId).toList();
        }
        //根据courseQuery中的create时间范围，和update时间范围查询is_delete为1且semester_id为semId的CourseDO
        Page<CourseDO> pageCourse=new Page<>(courseQuery.getPage(),courseQuery.getSize());
        QueryWrapper<CourseDO> courseWrapper=new QueryWrapper<>();
        courseWrapper.in("create_time",courseQuery.getQueryObj().getStartCreateTime(),courseQuery.getQueryObj().getEndCreateTime());
        courseWrapper.in("update_time",courseQuery.getQueryObj().getStartUpdateTime(),courseQuery.getQueryObj().getEndUpdateTime());
        if(semId!=null){
            courseWrapper.eq("semester_id",semId);
        }
        if(userIds!=null){
            courseWrapper.in("teacher_id",userIds);
        }
        pageCourse = courseMapper.selectPage(pageCourse,courseWrapper);
        //获取courseDOS中的subjectId集合
        List<Integer> subjectIds = pageCourse.getRecords().stream().map(CourseDO::getSubjectId).toList();
        //将数据库对象转换为subjectEntities业务对象
        List<SubjectEntity> subject = subjectMapper.selectList(new QueryWrapper<SubjectDO>().in("id", subjectIds))
                                                .stream().map(subjectDO -> courseConvertor.toSubjectEntity(subjectDO)).toList();


        //获取uerEntity
        //TODO 此处需要完善，目前仅返回空Optional


        //获取List<CourseEntity>
        List<CourseEntity> courseEntityList=new ArrayList<>();
        for(int i=0;i<pageCourse.getRecords().size();i++){
            courseEntityList.add(courseConvertor.toCourseEntity(pageCourse.getRecords().get(i),subject.get(i),null,semester));
        }










//        subjectMapper.


        return null;
    }

    @Override
    public Optional<CourseDetailCO> getCourseInfo(Integer id, Integer semId) {
        //根据id和semId来查询课程信息
        CourseDO courseDO = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", id));
        //根据id和semId来查询评教快照信息
        CourOneEvaTemplateDO courOneEvaTemplateDO = CoureOneEvaTemplateMapper.selectOne(new QueryWrapper<CourOneEvaTemplateDO>().eq("course_id", id).eq("semester_id", semId));
        //将courOneEvaTemplateDO中的formtemplate(json)字符串，转换为EvaTemplateCO
        EvaTemplateCO evaTemplateCO=null;
      if(courOneEvaTemplateDO.getFormTemplate()!=null){
          try {
              evaTemplateCO = new ObjectMapper().readValue(courOneEvaTemplateDO.getFormTemplate(), EvaTemplateCO.class);
          } catch (JsonProcessingException e) {
              throw new RuntimeException(e);
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
        List<CourseType> courseTypeList = courseTypes.stream().map(courseTypeDO -> courseConvertor.toCourseType(courseTypeDO)).toList();

        //组装CourseDtailCO
        CourseDetailCO courseDetailCO = courseConvertor.toCourseDetailCO(courseTypeList
                , courseTimeList, subjectDO, courseDO, evaTemplateCO, sysUserDO);
        return Optional.of(courseDetailCO);

    }
    /**
     * 根据课程ID和学期ID查询评价分数
     *
     * @param id   课程ID
     * @param semId 学期ID
     * @return 返回一个List，包含指定课程和学期的所有评价分数记录
     */
    @Override
    public List<CourseScoreCO> findEvaScore(Integer id, Integer semId) {

        //根据课程id来找到评教任务id
        QueryWrapper<EvaTaskDO> evaTaskWrapper = new QueryWrapper<>();
        evaTaskWrapper.eq("cour_inf_id", id);
        EvaTaskDO evaTaskDO = evaTaskMapper.selectOne(evaTaskWrapper);
        //根据评教任务id来找到评教表单记录数据
        List<FormRecordDO> evaRecordDOS = formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().eq("task_id", evaTaskDO.getId()));
        Map<String,List<FormRecordDO>> map=new HashMap<>();
        //TODO 根据prop来分组
        //问题是怎么从json中获取prop字段
        for(FormRecordDO formRecordDO:evaRecordDOS){
            String prop=formRecordDO.getFormPropsValues();
            try {
                CourseScoreCO courseScoreCO = new ObjectMapper().readValue(prop, CourseScoreCO.class);
                map.put(courseScoreCO.getProp(),map.getOrDefault(courseScoreCO.getProp(),new ArrayList<>()));
                map.get(courseScoreCO.getProp()).add(formRecordDO);
            } catch (JsonProcessingException e) {
               throw new RuntimeException(e);
            }
        }


        //将FormRecordDO转化成CourseScoreCO对象
        List<CourseScoreCO> courseScoreCOList = evaRecordDOS.stream()
                .map(formRecordDO -> {
                    try {
                        return new ObjectMapper().readValue(formRecordDO.getFormPropsValues(), CourseScoreCO.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();



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
        //根据学期semId和周数week来查询课程表（CourInfDO）再根据day来分组
        List<CourInfDO> list = courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("semester_id", semId).eq("week", week));
        Map<Integer,List<CourInfDO>> map=new HashMap<>();
        for (CourInfDO  courInfo : list) {
            if(map.containsKey(courInfo.getDay())){
                map.get(courInfo.getDay()).add(courInfo);
            }else{
                List<CourInfDO> list1=new ArrayList<>();
                list1.add(courInfo);
                map.put(courInfo.getDay(),list1);
            }
        }
        List<List<Integer>> result=new ArrayList<>();
       Map<Integer,Integer> mapNum=new HashMap<>();
        //根据day(星期几)来确定每个上课时间段，有多少节课
        for (Map.Entry<Integer, List<CourInfDO>> integerListEntry : map.entrySet()) {
            //存储每个上课时间段有多少节课
            List<Integer> newList=new ArrayList<>();
            //遍历每一天总共的课程，并且将每个时间段对应的课程数，放入mapNum中
            integerListEntry.getValue().stream().forEach(courInfDO -> {
                if(mapNum.containsKey(courInfDO.getStartTime())){
                    mapNum.put(courInfDO.getStartTime(),mapNum.get(courInfDO.getStartTime())+1);
                }else{
                   mapNum.put(courInfDO.getStartTime(),1);
                }
                });
            //遍历mapNum，将每个时间段对应的课程数，放入newList中
            mapNum.entrySet().stream().forEach(entry -> {
                newList.add(entry.getValue());
            });
            result.add(newList);
            //清空mapNum和newList
            mapNum.clear();
            newList.clear();
        }



        return null;
    }

    @Override
    public List<SingleCourseEntity> getPeriodInfo(Integer semId, CourseQuery courseQuery) {
        return null;
    }

    @Override
    public Optional<SingleCourseEntity> getSingleCourseDetail(Integer id, Integer semId) {
        return Optional.empty();
    }

    @Override
    public List<SingleCourseEntity> getPeriodCourse(Integer semId, CourseQuery courseQuery) {
        return null;
    }

    @Override
    public List<CourseTypeEntity> pageCourseType(PagingQuery<GenericConditionalQuery> courseQuery) {
        Page<CourseTypeDO> page =new Page<>(courseQuery.getPage(),courseQuery.getSize());
        QueryWrapper<CourseTypeDO> queryWrapper = new QueryWrapper<>();
            if(courseQuery.getQueryObj().getKeyword()!=null){
                queryWrapper.like("name",courseQuery.getQueryObj().getKeyword());
            }
            queryWrapper.between("create_time",courseQuery.getQueryObj().getStartCreateTime(),courseQuery.getQueryObj().getEndCreateTime());
            queryWrapper.between("update_time",courseQuery.getQueryObj().getStartUpdateTime(),courseQuery.getQueryObj().getEndUpdateTime());
        Page<CourseTypeDO> courseTypeDOPage = courseTypeMapper.selectPage(page, queryWrapper);
       return courseTypeDOPage.getRecords().stream().map(courseTypeDO -> courseConvertor.toCourseTypeEntity(courseTypeDO)).toList();



    }

    @Override
    public List<CourseEntity> getCourseDetail(Integer id, Integer semId) {
        return null;
    }

    @Override
    public List<SingleCourseEntity> getSelfCourse(Integer semId) {
        return null;
    }

    public SemesterEntity getSemester(Integer semId){
        SemesterDO semesterDO=null;
        if(semId!=null){
            semesterDO= semesterMapper.selectById(semId);
            return courseConvertor.toSemesterEntity(semesterDO);
        }
        return null;
    }
    public List<CoursePeriod> toGetCourseTime( Integer id){
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

}
