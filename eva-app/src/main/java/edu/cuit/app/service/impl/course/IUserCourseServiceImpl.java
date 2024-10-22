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
import edu.cuit.client.bo.MessageBO;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.course.RecommendCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import edu.cuit.client.dto.cmd.SendMessageCmd;
import edu.cuit.client.dto.data.Term;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.gateway.course.CourseDeleteGateway;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
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
    private final MsgResult msgResult;

    private final ObjectMapper objectMapper;


    @CheckSemId
    @Override
    public List<SimpleResultCO> getUserCourseInfo( Integer semId) {
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
        if(type==0){
            FileImportExec.importCourse(CourseExcelResolver.resolveData(CourseExcelResolver.Strategy.THEORY_COURSE, fileStream));
        }else if(type==1){
            FileImportExec.importCourse(CourseExcelResolver.resolveData(CourseExcelResolver.Strategy.EXPERIMENTAL_COURSE, fileStream));
        }else{
            throw new BizException("课表类型转换错误");
        }
        courseUpdateGateway.importCourseFile(FileImportExec.courseExce, semesterCO,type);

    }

    @CheckSemId
    @Override
    public List<SelfTeachCourseCO> selfCourseDetail(Integer semId) {

        return courseQueryGateway.getSelfCourseInfo(String.valueOf(StpUtil.getLoginId()), semId);
    }

    @Override
    public List<SelfTeachCourseTimeCO> selfCourseTime(Integer courseId) {
        List<SingleCourseEntity> selfCourseTime = courseQueryGateway.getSelfCourseTime(courseId);
       //根据SingleCourseEntity中的day和starTime来分组
        Map<String, List<SingleCourseEntity>> map = selfCourseTime.stream().collect(Collectors.groupingBy(course -> course.getDay() + "-" + course.getStartTime()+"-"+course.getEndTime()));
        List<SelfTeachCourseTimeCO> list=new ArrayList<>();
        for (Map.Entry<String, List<SingleCourseEntity>> entry : map.entrySet()) {
            SelfTeachCourseTimeCO time=new SelfTeachCourseTimeCO();
            time.setDay(entry.getValue().get(0).getDay());
            time.setDay(entry.getValue().get(0).getStartTime());
            time.setDay(entry.getValue().get(0).getEndTime());
            List<Integer> week=new ArrayList<>();
            for (SingleCourseEntity courseTime : entry.getValue()) {
                week.add(courseTime.getWeek());
            }
            time.setWeeks(week);
            list.add(time);
        }
        return list;
    }

    @Override
    public Void deleteSelfCourse(Integer courseId) {
        Map<String, List<Integer>> map = courseDeleteGateway.deleteSelfCourse(String.valueOf(StpUtil.getLoginId()), courseId);
        msgResult.toSendMsg(map);
        return null;
    }

    @Override
    public Void updateSelfCourse(SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeCO> timeList) {
        Map<String, Map<Integer, Integer>> mapMsg = courseUpdateGateway.updateSelfCourse(String.valueOf(StpUtil.getLoginId()), selfTeachCourseCO, timeList);
        for (Map.Entry<String, Map<Integer, Integer>> stringMapEntry : mapMsg.entrySet()) {
            SendMessageCmd sendMessageCmd=new SendMessageCmd();
//            sendMessageCmd.setMsg(stringMapEntry.getValue().toString());
            sendMessageCmd.setMsg(stringMapEntry.getKey());
            for (Map.Entry<Integer, Integer> mapEntry : stringMapEntry.getValue().entrySet()) {
                sendMessageCmd.setTaskId(mapEntry.getKey()).setMode(0)
                        .setRecipientId(mapEntry.getValue())
                        .setType(1)
                        .setIsShowName(1);
                msgService.handleUserSendMessage(sendMessageCmd);
            }
        }


//        msgService.handleUserSendMessage();
        return null;
    }

    @Override
    public Boolean isImported(Integer type, Term term) {

        return courseUpdateGateway.isImported(type, term);
    }
}
