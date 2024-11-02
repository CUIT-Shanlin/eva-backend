package edu.cuit.app.service.impl.course;

import cn.dev33.satoken.stp.StpUtil;
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
import edu.cuit.client.dto.clientobject.SimpleSubjectResultCO;
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
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ICourseDetailServiceImpl implements ICourseDetailService {
    private final CourseQueryGateway courseQueryGateway;
    private final CourseUpdateGateway courseUpdateGateway;
    private final CourseDeleteGateway courseDeleteGateway;
    private final UserQueryGateway userQueryGateway;
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
        Map<String, Map<Integer,Integer>> map = courseUpdateGateway.updateCourse(semId, updateCourseCmd);
        Optional<Integer> userId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId());
        msgResult.SendMsgToAll(map, userId.orElseThrow(() -> new QueryException("请先登录")));
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
        Map<String, Map<Integer,Integer>> map = courseDeleteGateway.deleteCourse(semId, id);
        Optional<Integer> userId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId());
        for (Map.Entry<String, Map<Integer, Integer>> stringListEntry : map.entrySet()) {
           Map<String,Map<Integer,Integer>> temMap=new HashMap<>();
            if(stringListEntry.getValue()==null){
                temMap.put(stringListEntry.getKey(),null);
                msgResult.SendMsgToAll(temMap,userId.orElseThrow(() -> new QueryException("请先登录")));
            }else if(!stringListEntry.getValue().isEmpty()){
                temMap.put(stringListEntry.getKey(),stringListEntry.getValue());
                msgResult.toNormalMsg(temMap,userId.orElseThrow(() -> new QueryException("请先登录")));
                stringListEntry.getValue().forEach((k,v)->msgService.deleteEvaMsg(k,null));

            }
        }

    }
}
