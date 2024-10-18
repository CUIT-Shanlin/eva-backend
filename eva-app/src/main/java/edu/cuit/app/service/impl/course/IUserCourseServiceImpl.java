package edu.cuit.app.service.impl.course;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.convertor.course.CourseConvertor;
import edu.cuit.app.resolver.course.CourseExcelResolver;

import edu.cuit.app.service.operate.course.query.UserCourseDetailQueryExec;
import edu.cuit.app.service.operate.course.update.FileImportExec;
import edu.cuit.client.api.course.IUserCourseService;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.course.RecommendCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.gateway.course.CourseDeleteGateway;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
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
    private final CourseConvertor courseConvertor;
    private final UserCourseDetailQueryExec userCourseDetailQueryExec;

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
            semesterCO = new ObjectMapper().readValue(semester, SemesterCO.class);
        } catch (JsonProcessingException e) {
            throw new ClassCastException("学期类型转换错");
        }
        if(type==0){
            FileImportExec.importCourse(CourseExcelResolver.resolveData(CourseExcelResolver.Strategy.THEORY_COURSE, fileStream));
        }else if(type==1){
            FileImportExec.importCourse(CourseExcelResolver.resolveData(CourseExcelResolver.Strategy.EXPERIMENTAL_COURSE, fileStream));
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
        courseDeleteGateway.deleteSelfCourse(String.valueOf(StpUtil.getLoginId()),courseId);
        return null;
    }

    @Override
    public Void updateSelfCourse(SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeCO> timeList) {
        courseUpdateGateway.updateSelfCourse(String.valueOf(StpUtil.getLoginId()),selfTeachCourseCO, timeList);
        return null;
    }
}
