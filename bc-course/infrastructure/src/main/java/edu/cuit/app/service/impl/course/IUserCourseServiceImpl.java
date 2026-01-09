package edu.cuit.app.service.impl.course;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.cola.exception.BizException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cuit.app.aop.CheckSemId;

import edu.cuit.app.convertor.course.CourseBizConvertor;
import edu.cuit.app.event.AfterCommitEventPublisher;

import edu.cuit.bc.course.application.port.CourseExcelResolvePort;
import edu.cuit.bc.course.application.usecase.DeleteSelfCourseEntryUseCase;
import edu.cuit.bc.course.application.usecase.ImportCourseFileEntryUseCase;
import edu.cuit.bc.course.application.usecase.IsCourseImportedUseCase;
import edu.cuit.bc.course.application.usecase.UpdateSelfCourseEntryUseCase;
import edu.cuit.bc.messaging.application.event.CourseOperationMessageMode;
import edu.cuit.bc.messaging.application.event.CourseOperationSideEffectsEvent;
import edu.cuit.client.api.course.IUserCourseService;
import edu.cuit.client.bo.CourseExcelBO;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.client.dto.clientobject.SimpleSubjectResultCO;
import edu.cuit.client.dto.clientobject.course.*;
import edu.cuit.client.dto.data.Term;
import edu.cuit.client.dto.data.course.CoursePeriod;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
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
    private final CourseBizConvertor courseConvertor;
    private final UserQueryGateway userQueryGateway;
    private final AfterCommitEventPublisher afterCommitEventPublisher;

    private final ObjectMapper objectMapper;
    private final CourseExcelResolvePort courseExcelResolvePort;
    private final DeleteSelfCourseEntryUseCase deleteSelfCourseEntryUseCase;
    private final UpdateSelfCourseEntryUseCase updateSelfCourseEntryUseCase;
    private final ImportCourseFileEntryUseCase importCourseFileEntryUseCase;
    private final IsCourseImportedUseCase isCourseImportedUseCase;


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
           result.add(buildUserCourseDetail(singleCourseEntities, semId));
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
            courseExce = importCourse(courseExcelResolvePort.resolveTheoryCourse(fileStream));
        }else if(type==1){
            courseExce = importCourse(courseExcelResolvePort.resolveExperimentalCourse(fileStream));
        }else{
            throw new BizException("课表类型转换错误");
        }
        Map<String, Map<Integer,Integer>> map = importCourseFileEntryUseCase.importCourseFile(courseExce, semesterCO, type);
        Integer operatorUserId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId())
                .orElseThrow(() -> new QueryException("请先登录"));

        // 渐进式 DDD 重构：把“导入课表”的跨域副作用（消息通知、撤回评教消息）事件化交给 bc-messaging 处理
        afterCommitEventPublisher.publishAfterCommit(new CourseOperationSideEffectsEvent(operatorUserId, map));

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

    private static Map<String, List<CourseExcelBO>> importCourse(List<CourseExcelBO> list){
        Map<String, List<CourseExcelBO>> courseExce = new HashMap<>();
        //根据CourseExcelBO中的课程名称进行分类
        for (CourseExcelBO entity : list) {
            String courseName = entity.getCourseName();
            courseExce.computeIfAbsent(courseName, k -> new ArrayList<>()).add(entity);
        }
        return courseExce;
    }

    private CourseDetailCO buildUserCourseDetail(List<SingleCourseEntity> singleCourseEntities,Integer semId){
        List<CourseType> typeList=null;
        CourseModelCO courseModelCO=null;
        if(!singleCourseEntities.isEmpty()){
            typeList = courseQueryGateway.getCourseType(singleCourseEntities.get(0).getCourseEntity().getId());
//             List<String> location=si
            courseModelCO = courseConvertor.toCourseModelCO(singleCourseEntities.get(0).getCourseEntity(), courseQueryGateway.getLocation(singleCourseEntities.get(0).getCourseEntity().getId()));
        }

        //根据singleCourseEntities中的上课的星期数和startTime以及endTime，将课程分类
        Map<String, List<SingleCourseEntity>> courseByDay = new HashMap<>();
        for (SingleCourseEntity entity : singleCourseEntities) {
            String dayOfWeek = entity.getDay().toString()+entity.getStartTime().toString()+entity.getEndTime().toString();
            courseByDay.computeIfAbsent(dayOfWeek, k -> new ArrayList<>()).add(entity);
        }
        List<CoursePeriod> coursePeriodList = getCoursePeriods(courseByDay);


        return new CourseDetailCO().setCourseBaseMsg(courseModelCO).setDateList(coursePeriodList).setTypeList(typeList);
    }

    private static List<CoursePeriod> getCoursePeriods(Map<String, List<SingleCourseEntity>> courseByDay) {
        List<CoursePeriod> coursePeriodList = new ArrayList<>();
        CoursePeriod temp=new CoursePeriod();
        for (Map.Entry<String, List<SingleCourseEntity>> entry : courseByDay.entrySet()) {
            temp.setStartTime(entry.getValue().get(0).getStartTime());
            temp.setEndTime(entry.getValue().get(0).getEndTime());
            temp.setDay(entry.getValue().get(0).getDay());
            int startWeek=entry.getValue().get(0).getWeek();
            int endWeek=entry.getValue().get(0).getWeek();
            for (SingleCourseEntity singleCourseEntity : entry.getValue()) {
                if(singleCourseEntity.getWeek()>=endWeek){
                    endWeek=singleCourseEntity.getWeek();
                }
                if(singleCourseEntity.getWeek()<=startWeek){
                    startWeek=singleCourseEntity.getWeek();
                }
            }
            temp.setStartWeek(startWeek);
            temp.setEndWeek(endWeek);
            coursePeriodList.add(temp);
            //清空temp
            temp=new CoursePeriod();
        }
        return coursePeriodList;
    }

    @Override
    public Void deleteSelfCourse(Integer courseId) {
        String userName = String.valueOf(StpUtil.getLoginId());
        Map<String, Map<Integer, Integer>> map = deleteSelfCourseEntryUseCase.deleteSelfCourse(userName, courseId);
        Integer operatorUserId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId())
                .orElseThrow(() -> new QueryException("请先登录"));

        // 渐进式 DDD 重构：把“教师自助删课”的跨域副作用（消息通知、撤回评教消息）事件化交给 bc-messaging 处理
        afterCommitEventPublisher.publishAfterCommit(new CourseOperationSideEffectsEvent(operatorUserId, map));
        return null;
    }

    @Override
    public Void updateSelfCourse(SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeInfoCO> timeList) {
        String userName = String.valueOf(StpUtil.getLoginId());
        Map<String, Map<Integer, Integer>> mapMsg = updateSelfCourseEntryUseCase.updateSelfCourse(userName, selfTeachCourseCO, timeList);
        Integer operatorUserId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId())
                .orElseThrow(() -> new QueryException("请先登录"));

        // 渐进式 DDD 重构：保持历史“携带 taskId 的消息”行为不变，事件化交给 bc-messaging 处理
        afterCommitEventPublisher.publishAfterCommit(
                new CourseOperationSideEffectsEvent(operatorUserId, mapMsg, CourseOperationMessageMode.TASK_LINKED)
        );
        return null;
    }

    @Override
    public Boolean isImported(Integer type, Term term) {
        return isCourseImportedUseCase.execute(type, term);
    }

    @Override
    public List<Integer> getUserCourses(Integer semId, Integer userId) {
        return courseQueryGateway.getUserCourses(semId, userId);
    }
}
