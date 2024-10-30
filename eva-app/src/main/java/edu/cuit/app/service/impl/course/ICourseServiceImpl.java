package edu.cuit.app.service.impl.course;

import cn.dev33.satoken.stp.StpUtil;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.convertor.course.CourseBizConvertor;
import edu.cuit.app.service.impl.MsgServiceImpl;
import edu.cuit.app.service.operate.course.MsgResult;
import edu.cuit.client.api.course.ICourseService;
import edu.cuit.client.bo.MessageBO;
import edu.cuit.client.dto.clientobject.course.*;
import edu.cuit.client.dto.clientobject.eva.EvaTeacherInfoCO;
import edu.cuit.client.dto.cmd.course.AlignTeacherCmd;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import edu.cuit.client.dto.cmd.course.UpdateSingleCourseCmd;
import edu.cuit.client.dto.data.course.CoursePeriod;
import edu.cuit.client.dto.query.CourseQuery;
import edu.cuit.client.dto.query.condition.MobileCourseQuery;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.gateway.course.CourseDeleteGateway;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ICourseServiceImpl implements ICourseService {
    private final CourseQueryGateway courseQueryGateway;
    private final CourseUpdateGateway courseUpdateGateway;
    private final CourseDeleteGateway courseDeleteGateway;
    private final CourseBizConvertor courseConvertor;
    private final UserQueryGateway userQueryGateway;
    private final MsgResult msgResult;
    @CheckSemId
    @Override
    public List<List<Integer>> courseNum(Integer semId, Integer week) {
       return courseQueryGateway.getWeekCourses(week,semId);

    }

    @CheckSemId
    @Override
    public List<SingleCourseCO> courseTimeDetail(Integer semId, CourseQuery courseQuery) {
        return courseQueryGateway.getPeriodInfo(semId,courseQuery);

    }

    @CheckSemId
    @Override
    public SingleCourseDetailCO getCourseDetail(Integer semId, Integer id) {
        List<EvaTeacherInfoCO> evaUsers = courseQueryGateway.getEvaUsers(id);
        if(evaUsers==null){
            evaUsers=new ArrayList<>();
        }
        List<EvaTeacherInfoCO> finalEvaUsers = evaUsers;
        return courseQueryGateway.getSingleCourseDetail(id, semId)
                .map(courseEntity -> courseConvertor.toSingleCourseDetailCO(courseEntity, finalEvaUsers))
                .orElseThrow(()->new QueryException("这节课不存在"));
    }

    @CheckSemId
    @Override
    public String getDate(Integer semId, Integer week, Integer day) {
       return courseQueryGateway.getDate(semId,week,day);

    }

    @CheckSemId
    @Override
    public List<RecommendCourseCO> getTimeCourse(Integer semId, MobileCourseQuery courseQuery) {
        String userName =String.valueOf(StpUtil.getLoginId());
        return courseQueryGateway.getPeriodCourse(semId, courseQuery,userName);
    }

    @CheckSemId
    @Override
    public void updateSingleCourse(Integer semId, UpdateSingleCourseCmd updateSingleCourseCmd) {
        String userName =String.valueOf(StpUtil.getLoginId()) ;
        Map<String, List<Integer>> map = courseUpdateGateway.updateSingleCourse(userName, semId, updateSingleCourseCmd);
        Optional<Integer> userId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId());
        msgResult.toSendMsg(map, userId.orElseThrow(() -> new QueryException("请先登录")));

    }

    @CheckSemId
    @Override
    public void allocateTeacher(Integer semId, AlignTeacherCmd alignTeacherCmd) {
        Map<String, List<Integer>> map = courseUpdateGateway.assignTeacher(semId, alignTeacherCmd);
        Optional<Integer> userId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId());
        msgResult.toSendMsg(map, userId.orElseThrow(() -> new QueryException("请先登录")));

    }

    @CheckSemId
    @Override
    public void deleteCourses(Integer semId, Integer id, CoursePeriod coursePeriod) {
        Map<String, List<Integer>> map = courseDeleteGateway.deleteCourses(semId, id, coursePeriod);
        Optional<Integer> userId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId());
        msgResult.toSendMsg(map, userId.orElseThrow(() -> new QueryException("请先登录")));

    }

    @Override
    public void addExistCoursesDetails(Integer courseId, SelfTeachCourseTimeCO timeCO) {
            courseUpdateGateway.addExistCoursesDetails(courseId,timeCO);
    }

    @CheckSemId
    @Override
    public void addNotExistCoursesDetails(Integer semId, Integer teacherId, UpdateCourseCmd courseInfo, List<SelfTeachCourseTimeCO> dateArr) {
        courseUpdateGateway.addNotExistCoursesDetails(semId,teacherId,courseInfo,dateArr);
    }
}
