package edu.cuit.app.service.impl.course;

import cn.dev33.satoken.stp.StpUtil;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.event.AfterCommitEventPublisher;
import edu.cuit.bc.course.application.usecase.AddNotExistCoursesDetailsEntryUseCase;
import edu.cuit.bc.course.application.usecase.CourseDetailQueryUseCase;
import edu.cuit.bc.course.application.usecase.CourseQueryUseCase;
import edu.cuit.bc.course.application.usecase.TimeCourseQueryUseCase;
import edu.cuit.bc.course.application.usecase.AllocateTeacherUseCase;
import edu.cuit.bc.course.application.usecase.DeleteCoursesEntryUseCase;
import edu.cuit.bc.course.application.usecase.UpdateSingleCourseEntryUseCase;
import edu.cuit.bc.messaging.application.event.CourseOperationSideEffectsEvent;
import edu.cuit.bc.messaging.application.event.CourseTeacherTaskMessagesEvent;
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
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ICourseServiceImpl implements ICourseService {
    private final CourseQueryGateway courseQueryGateway;
    private final CourseUpdateGateway courseUpdateGateway;
    private final CourseQueryUseCase courseQueryUseCase;
    private final CourseDetailQueryUseCase courseDetailQueryUseCase;
    private final TimeCourseQueryUseCase timeCourseQueryUseCase;
    private final AllocateTeacherUseCase allocateTeacherUseCase;
    private final DeleteCoursesEntryUseCase deleteCoursesEntryUseCase;
    private final UpdateSingleCourseEntryUseCase updateSingleCourseEntryUseCase;
    private final AddNotExistCoursesDetailsEntryUseCase addNotExistCoursesDetailsEntryUseCase;
    private final UserQueryGateway userQueryGateway;
    private final AfterCommitEventPublisher afterCommitEventPublisher;
    @CheckSemId
    @Override
    public List<List<Integer>> courseNum(Integer week, Integer semId) {
       return courseQueryUseCase.courseNum(week, semId);

    }

    @CheckSemId
    @Override
    public List<SingleCourseCO> courseTimeDetail(Integer semId, CourseQuery courseQuery) {
        return courseQueryUseCase.courseTimeDetail(semId, courseQuery);

    }

    @CheckSemId
    @Override
    public SingleCourseDetailCO getCourseDetail(Integer semId, Integer id) {
        return courseDetailQueryUseCase.getCourseDetail(semId, id)
                .orElseThrow(() -> new QueryException("这节课不存在"));
    }

    @CheckSemId
    @Override
    public String getDate(Integer semId, Integer week, Integer day) {
       return courseQueryUseCase.getDate(semId, week, day);

    }

    @CheckSemId
    @Override
    public List<RecommendCourseCO> getTimeCourse(Integer semId, MobileCourseQuery courseQuery) {
        String userName =String.valueOf(StpUtil.getLoginId());
        return timeCourseQueryUseCase.getTimeCourse(semId, courseQuery, userName);
    }

    @CheckSemId
    @Override
    public void updateSingleCourse(Integer semId, UpdateSingleCourseCmd updateSingleCourseCmd) {
        String userName =String.valueOf(StpUtil.getLoginId()) ;
        Map<String,Map<Integer,Integer>> map = updateSingleCourseEntryUseCase.updateSingleCourse(userName, semId, updateSingleCourseCmd);
        Optional<Integer> userId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId());
        Integer operatorUserId = userId.orElseThrow(() -> new QueryException("请先登录"));
        afterCommitEventPublisher.publishAfterCommit(new CourseOperationSideEffectsEvent(operatorUserId, map));
    }

    @CheckSemId
    @Override
    public void allocateTeacher(Integer semId, AlignTeacherCmd alignTeacherCmd) {
        Map<String, Map<Integer,Integer>> map = allocateTeacherUseCase.allocateTeacher(semId, alignTeacherCmd);
        Optional<Integer> userId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId());
        Integer operatorUserId = userId.orElseThrow(() -> new QueryException("请先登录"));
        afterCommitEventPublisher.publishAfterCommit(new CourseTeacherTaskMessagesEvent(operatorUserId, map));
    }

    @CheckSemId
    @Override
    public void deleteCourses(Integer semId, Integer id, CoursePeriod coursePeriod) {
        Map<String, Map<Integer,Integer>> map = deleteCoursesEntryUseCase.deleteCourses(semId, id, coursePeriod);
        Optional<Integer> userId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId());
        Integer operatorUserId = userId.orElseThrow(() -> new QueryException("请先登录"));
        afterCommitEventPublisher.publishAfterCommit(new CourseOperationSideEffectsEvent(operatorUserId, map));

    }

    @Override
    public void addExistCoursesDetails(Integer courseId, SelfTeachCourseTimeCO timeCO) {
            courseUpdateGateway.addExistCoursesDetails(courseId,timeCO);
    }

    @CheckSemId
    @Override
    public void addNotExistCoursesDetails(Integer semId, Integer teacherId, UpdateCourseCmd courseInfo, List<SelfTeachCourseTimeCO> dateArr) {
        addNotExistCoursesDetailsEntryUseCase.addNotExistCoursesDetails(semId, teacherId, courseInfo, dateArr);
    }
}
