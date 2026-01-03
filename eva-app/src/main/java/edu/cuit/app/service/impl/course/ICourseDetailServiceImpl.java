package edu.cuit.app.service.impl.course;

import cn.dev33.satoken.stp.StpUtil;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.app.convertor.course.CourseBizConvertor;
import edu.cuit.app.event.AfterCommitEventPublisher;
import edu.cuit.bc.course.application.model.ChangeSingleCourseTemplateCommand;
import edu.cuit.bc.course.application.usecase.ChangeSingleCourseTemplateUseCase;
import edu.cuit.bc.course.application.usecase.AddCourseEntryUseCase;
import edu.cuit.bc.course.application.usecase.DeleteCourseEntryUseCase;
import edu.cuit.bc.course.application.usecase.UpdateCourseEntryUseCase;
import edu.cuit.bc.course.application.usecase.UpdateCoursesEntryUseCase;
import edu.cuit.bc.course.domain.ChangeCourseTemplateException;
import edu.cuit.bc.course.domain.CourseNotFoundException;
import edu.cuit.bc.messaging.application.event.CourseOperationSideEffectsEvent;
import edu.cuit.client.api.course.ICourseDetailService;
import edu.cuit.client.bo.MessageBO;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.SimpleCourseResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.SimpleSubjectResultCO;
import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.course.CourseModelCO;
import edu.cuit.client.dto.clientobject.eva.CourseScoreCO;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.client.dto.cmd.SendMessageCmd;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import edu.cuit.client.dto.cmd.course.UpdateCoursesCmd;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.CourseConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.course.CourseEntity;
import edu.cuit.domain.entity.course.SubjectEntity;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.infra.gateway.impl.course.operate.CourseFormat;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ICourseDetailServiceImpl implements ICourseDetailService {
    private final CourseQueryGateway courseQueryGateway;
    private final UserQueryGateway userQueryGateway;
    private final CourseBizConvertor courseBizConvertor;
    private final PaginationBizConvertor pagenConvertor;
   private final CourseFormat courseFormat;
    private final ChangeSingleCourseTemplateUseCase changeSingleCourseTemplateUseCase;
    private final AfterCommitEventPublisher afterCommitEventPublisher;
    private final UpdateCourseEntryUseCase updateCourseEntryUseCase;
    private final UpdateCoursesEntryUseCase updateCoursesEntryUseCase;
    private final DeleteCourseEntryUseCase deleteCourseEntryUseCase;
    private final AddCourseEntryUseCase addCourseEntryUseCase;
    @CheckSemId
    @Override
    public PaginationQueryResultCO<CourseModelCO> pageCoursesInfo(Integer semId, PagingQuery<CourseConditionalQuery> courseQuery) {
        PaginationResultEntity<CourseEntity> page = courseQueryGateway.page(courseQuery, semId);
        List<CourseEntity> records = page.getRecords();
        List<CourseModelCO> list=new ArrayList<>();
        for (CourseEntity record : records) {
            List<String> location = courseQueryGateway.getLocation(record.getId());
            EvaTemplateCO evaTemplateCO = courseFormat.selectCourOneEvaTemplateDO(semId, record.getId());
            if(evaTemplateCO==null) {
                list.add(courseBizConvertor.toCourseModelCO(record, location));
            }
            else{
                CourseModelCO courseModelCO2 = courseBizConvertor.toCourseModelCO2(record, location);
                courseModelCO2.setTemplateMsg(evaTemplateCO);
                list.add(courseModelCO2);
            }
        }
        return pagenConvertor.toPaginationEntity(page, list);
    }

    @CheckSemId
    @Override
    public CourseDetailCO courseInfo(Integer id, Integer semId) {
       return courseQueryGateway.getCourseInfo(id, semId).orElseThrow(()->new QueryException("课程不存在"));

    }

    @Override
    public List<CourseScoreCO> evaResult(Integer id) {
        return courseQueryGateway.findEvaScore(id);
    }

    @CheckSemId
    @Override
    public List<SimpleCourseResultCO> allCourseInfo(Integer semId) {
        PaginationResultEntity<CourseEntity> page = courseQueryGateway.page(null, semId);
        return page.getRecords().stream().map(courseBizConvertor::toSimpleCourseResultCO).toList();


    }

    @Override
    public List<SimpleSubjectResultCO> allSubjectInfo() {
        List<SubjectEntity> subject = courseQueryGateway.findSubjectInfo();
        return subject.stream().map(courseBizConvertor::toSimpleSubjectResultCO).toList();
    }

    @CheckSemId
    @Override
    public void updateCourse(Integer semId, UpdateCourseCmd updateCourseCmd) {
        // 单课程模板切换：前置交给 bc-course 用例，避免基础设施层重复实现与重复校验
        try {
            changeSingleCourseTemplateUseCase.execute(new ChangeSingleCourseTemplateCommand(
                    semId,
                    updateCourseCmd.getId(),
                    updateCourseCmd.getTemplateId()
            ));
        } catch (CourseNotFoundException e) {
            throw new QueryException(e.getMessage());
        } catch (ChangeCourseTemplateException e) {
            throw new UpdateException(e.getMessage());
        }

        Map<String, Map<Integer,Integer>> map = updateCourseEntryUseCase.updateCourse(semId, updateCourseCmd);
        Optional<Integer> userId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId());
        Integer operatorUserId = userId.orElseThrow(() -> new QueryException("请先登录"));
        afterCommitEventPublisher.publishAfterCommit(new CourseOperationSideEffectsEvent(operatorUserId, map));
    }

    @CheckSemId
    @Override
    @Transactional
    public void updateCourses(Integer semId, UpdateCoursesCmd updateCoursesCmd) {
        try {
            updateCoursesEntryUseCase.updateCourses(semId, updateCoursesCmd);
        } catch (ChangeCourseTemplateException e) {
            throw new UpdateException(e.getMessage());
        }
    }

    @CheckSemId
    @Override
    public void addCourse(Integer semId) {
        addCourseEntryUseCase.addCourse(semId);
    }

    @CheckSemId
    @Override
    public void delete(Integer semId, Integer id) {
        Map<String, Map<Integer,Integer>> map = deleteCourseEntryUseCase.deleteCourse(semId, id);
        Optional<Integer> userId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId());
        Integer operatorUserId = userId.orElseThrow(() -> new QueryException("请先登录"));
        afterCommitEventPublisher.publishAfterCommit(new CourseOperationSideEffectsEvent(operatorUserId, map));

    }
}
