package edu.cuit.app.service.impl.course;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.cola.exception.BizException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cuit.app.aop.CheckSemId;

import edu.cuit.app.convertor.course.CourseBizConvertor;
import edu.cuit.app.resolver.course.CourseExcelResolver;

import edu.cuit.app.service.impl.MsgServiceImpl;
import edu.cuit.app.service.operate.course.MsgResult;
import edu.cuit.app.service.operate.course.query.UserCourseDetailQueryExec;
import edu.cuit.app.service.operate.course.update.FileImportExec;
import edu.cuit.client.api.course.IUserCourseService;
import edu.cuit.client.bo.CourseExcelBO;
import edu.cuit.client.bo.MessageBO;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.client.dto.clientobject.SimpleSubjectResultCO;
import edu.cuit.client.dto.clientobject.course.*;
import edu.cuit.client.dto.data.Term;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.gateway.course.CourseDeleteGateway;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IUserCourseServiceImpl implements IUserCourseService {
    private final CourseQueryGateway courseQueryGateway;
    private final CourseUpdateGateway courseUpdateGateway;
    private final CourseDeleteGateway courseDeleteGateway;
    private final CourseBizConvertor courseConvertor;
    private final UserCourseDetailQueryExec userCourseDetailQueryExec;
    private final MsgServiceImpl msgService;
    private final UserQueryGateway userQueryGateway;
    private final MsgResult msgResult;

    private final ObjectMapper objectMapper;


    @CheckSemId
    @Override
    public List<SimpleSubjectResultCO> getUserCourseInfo(Integer semId) {
        String userName = String.valueOf(StpUtil.getLoginId());
        List<SelfTeachCourseCO> selfCourseInfo = courseQueryGateway.getSelfCourseInfo(userName, semId);

        return selfCourseInfo.stream().map(courseConvertor::toSimpleResultCO).toList();
    }

    @CheckSemId
    public List<CourseDetailCO> getUserCourseDetail(Integer id, Integer semId) {
        List<List<SingleCourseEntity>> courseList = courseQueryGateway.getUserCourseDetail(id, semId);
        List<CourseDetailCO> result=new ArrayList<>();
        for (List<SingleCourseEntity> singleCourseEntities : courseList) {
           result.add(userCourseDetailQueryExec.getUserCourseDetail(singleCourseEntities, semId));
        }
        return result;
    }

    @CheckSemId
    @Override
    public List<RecommendCourseCO> getSelfCourse(Integer semId) {
       return courseQueryGateway.getSelfCourse(semId, String.valueOf(StpUtil.getLoginId()));
    }

    @Override
    public void importCourse(InputStream fileStream, Integer type, String semester) {
        SemesterCO semesterCO=null;
        try {
            semesterCO = objectMapper.readValue(semester, SemesterCO.class);
        } catch (JsonProcessingException e) {
            throw new UpdateException("学期类型转换错误");
        }
        Map<String, List<CourseExcelBO>> courseExce;
        if(type==0){
            courseExce = FileImportExec.importCourse(CourseExcelResolver.resolveData(CourseExcelResolver.Strategy.THEORY_COURSE, fileStream));
        }else if(type==1){
            courseExce = FileImportExec.importCourse(CourseExcelResolver.resolveData(CourseExcelResolver.Strategy.EXPERIMENTAL_COURSE, fileStream));
        }else{
            throw new BizException("课表类型转换错误");
        }
        Map<String, Map<Integer,Integer>> map = courseUpdateGateway.importCourseFile(courseExce, semesterCO, type);
        Optional<Integer> userId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId());
        for (Map.Entry<String, Map<Integer, Integer>> stringListEntry : map.entrySet()) {
            Map<String, Map<Integer, Integer> > temMap=new HashMap<>();
            temMap.put(stringListEntry.getKey(), stringListEntry.getValue());
            if(stringListEntry.getValue()!=null){
                if(!stringListEntry.getValue().isEmpty()) {
                    msgResult.toNormalMsg(temMap, userId.orElseThrow(() -> new QueryException("请先登录")));
                    for (Map.Entry<Integer, Integer> k : stringListEntry.getValue().entrySet()) {
                        msgService.deleteEvaMsg(k.getKey(),null);
                    }
                }
                }else{
                msgResult.SendMsgToAll(temMap,userId.orElseThrow(() -> new QueryException("请先登录")));

            }

        }

    }

    @CheckSemId
    @Override
    public List<SelfTeachCourseCO> selfCourseDetail(Integer semId) {

        return courseQueryGateway.getSelfCourseInfo(String.valueOf(StpUtil.getLoginId()), semId);
    }

    @Override
    public List<SelfTeachCourseTimeInfoCO> selfCourseTime(Integer courseId) {
        List<SingleCourseEntity> selfCourseTime = courseQueryGateway.getSelfCourseTime(courseId);
       //根据SingleCourseEntity中的day和starTime来分组
        Map<String, List<SingleCourseEntity>> map = selfCourseTime.stream().collect(Collectors.groupingBy(course -> course.getDay() + "-" + course.getStartTime()+"-"+course.getEndTime()));
        List<SelfTeachCourseTimeInfoCO> list=new ArrayList<>();
        for (Map.Entry<String, List<SingleCourseEntity>> entry : map.entrySet()) {
            SelfTeachCourseTimeInfoCO time=new SelfTeachCourseTimeInfoCO();
            time.setDay(entry.getValue().get(0).getDay());
            time.setStartTime(entry.getValue().get(0).getStartTime());
            time.setEndTime(entry.getValue().get(0).getEndTime());
            List<String> location = entry.getValue().stream().map(SingleCourseEntity::getLocation).distinct().toList();
            time.setClassroom(location);
//            time.setClassroom(entry.getValue().get(0).getLocation());
            List<Integer> week=new ArrayList<>();
            for (SingleCourseEntity courseTime : entry.getValue()) {
                week.add(courseTime.getWeek());
            }
            time.setWeeks(week.stream().distinct().toList());
            list.add(time);
        }
        return list;
    }

    @Override
    public Void deleteSelfCourse(Integer courseId) {
        Map<String, Map<Integer,Integer>> map = courseDeleteGateway.deleteSelfCourse(String.valueOf(StpUtil.getLoginId()), courseId);
        Optional<Integer> userId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId());
        for (Map.Entry<String, Map<Integer, Integer>> stringMapEntry : map.entrySet()) {
            Map<String,Map<Integer,Integer>> map1=new HashMap<>();
            map1.put(stringMapEntry.getKey(),stringMapEntry.getValue());
            if(stringMapEntry.getValue()==null){
                msgResult.SendMsgToAll(map1, userId.orElseThrow(() -> new QueryException("请先登录")));
            } else if (!stringMapEntry.getValue().isEmpty()) {
                msgResult.toNormalMsg(map1, userId.orElseThrow(() -> new QueryException("请先登录")));
                stringMapEntry.getValue().forEach((k,v)->msgService.deleteEvaMsg(k,null));
            }

        }


        return null;
    }

    @Override
    public Void updateSelfCourse(SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeInfoCO> timeList) {
        Map<String, Map<Integer, Integer>> mapMsg = courseUpdateGateway.updateSelfCourse(String.valueOf(StpUtil.getLoginId()), selfTeachCourseCO, timeList);
        Optional<Integer> userId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId());
        for (Map.Entry<String, Map<Integer, Integer>> stringMapEntry : mapMsg.entrySet()) {
            Map<String,Map<Integer,Integer>> map=new HashMap<>();
            map.put(stringMapEntry.getKey(),stringMapEntry.getValue());
            if(stringMapEntry.getValue()==null&&stringMapEntry.getKey()!=null&& !stringMapEntry.getKey().isEmpty()){
                msgResult.SendMsgToAll(map, userId.orElseThrow(() -> new QueryException("请先登录")));
            }else if(!Objects.equals(stringMapEntry.getKey(), "")){
                MessageBO messageBO=new MessageBO();
                messageBO.setMsg(stringMapEntry.getKey());
            for (Map.Entry<Integer, Integer> mapEntry : stringMapEntry.getValue().entrySet()) {
                messageBO.setTaskId(mapEntry.getKey()).setMode(0)
                        .setTaskId(mapEntry.getKey())
                        .setRecipientId(mapEntry.getValue())
                        .setType(1)
                        .setIsShowName(1)
                        .setSenderId(userId.orElseThrow(()->new QueryException("请先登录")));
                msgService.sendMessage(messageBO);
                msgService.deleteEvaMsg(mapEntry.getKey(),null);
            }
            }

        }

        return null;
    }

    @Override
    public Boolean isImported(Integer type, Term term) {

        return courseUpdateGateway.isImported(type, term);
    }

    @Override
    public List<Integer> getUserCourses(Integer semId, Integer userId) {
        return courseQueryGateway.getUserCourses(semId, userId);
    }
}
