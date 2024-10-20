package edu.cuit.app.service.impl.course;

import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.app.convertor.course.CourseBizConvertor;
import edu.cuit.app.service.impl.MsgServiceImpl;
import edu.cuit.app.service.operate.course.MsgResult;
import edu.cuit.client.api.course.ICourseDetailService;
import edu.cuit.client.bo.MessageBO;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.SimpleCourseResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.course.CourseModelCO;
import edu.cuit.client.dto.clientobject.eva.CourseScoreCO;
import edu.cuit.client.dto.cmd.SendMessageCmd;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import edu.cuit.client.dto.cmd.course.UpdateCoursesCmd;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.CourseConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.course.CourseEntity;
import edu.cuit.domain.entity.course.SubjectEntity;
import edu.cuit.domain.gateway.course.CourseDeleteGateway;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ICourseDetailServiceImpl implements ICourseDetailService {
    private final CourseQueryGateway courseQueryGateway;
    private final CourseUpdateGateway courseUpdateGateway;
    private final CourseDeleteGateway courseDeleteGateway;
    private final CourseBizConvertor courseBizConvertor;
    private final PaginationBizConvertor pagenConvertor;
    private final MsgServiceImpl msgService;
   private final MsgResult msgResult;
    @CheckSemId
    @Override
    public PaginationQueryResultCO<CourseModelCO> pageCoursesInfo(Integer semId, PagingQuery<CourseConditionalQuery> courseQuery) {
        PaginationResultEntity<CourseEntity> page = courseQueryGateway.page(courseQuery, semId);
        List<CourseEntity> records = page.getRecords();
        List<CourseModelCO> list=new ArrayList<>();
        for (CourseEntity record : records) {
            List<String> location = courseQueryGateway.getLocation(record.getId());
            list.add(courseBizConvertor.toCourseModelCO(record, location));
        }
        return pagenConvertor.toPaginationEntity(page, list);
    }

    @CheckSemId
    @Override
    public CourseDetailCO courseInfo(Integer id, Integer semId) {
       return courseQueryGateway.getCourseInfo(id, semId).orElseThrow(()->new QueryException("课程不存在"));

    }

    @CheckSemId
    @Override
    public List<CourseScoreCO> evaResult(Integer id, Integer semId) {
        return courseQueryGateway.findEvaScore(id, semId);
    }

    @CheckSemId
    @Override
    public List<SimpleCourseResultCO> allCourseInfo(Integer semId) {
        PaginationResultEntity<CourseEntity> page = courseQueryGateway.page(null, semId);
        return page.getRecords().stream().map(courseBizConvertor::toSimpleCourseResultCO).toList();


    }

    @Override
    public List<SimpleResultCO> allSubjectInfo() {
        List<SubjectEntity> subject = courseQueryGateway.findSubjectInfo();
        return subject.stream().map(courseBizConvertor::toSimpleResultCO).toList();
    }

    @CheckSemId
    @Override
    public void updateCourse(Integer semId, UpdateCourseCmd updateCourseCmd) {
        String msg = courseUpdateGateway.updateCourse(semId, updateCourseCmd);
        msgService.sendMessage(new MessageBO().setMsg(msg)
                .setMode(0).setIsShowName(1)
                .setRecipientId(null).setSenderId(null)
                .setType(1));
    }

    @CheckSemId
    @Override
    public void updateCourses(Integer semId, UpdateCoursesCmd updateCoursesCmd) {
      courseUpdateGateway.updateCourses(semId, updateCoursesCmd);
    }

    @CheckSemId
    @Override
    public void addCourse(Integer semId) {
        courseUpdateGateway.addCourse(semId);
    }

    @CheckSemId
    @Override
    public void delete(Integer semId, Integer id) {

        Map<String, List<Integer>> map = courseDeleteGateway.deleteCourse(semId, id);
       msgResult.toSendMsg(map);
    }
}
