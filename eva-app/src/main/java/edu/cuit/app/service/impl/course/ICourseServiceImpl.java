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
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ICourseServiceImpl implements ICourseService {
    private final CourseQueryGateway courseQueryGateway;
    private final CourseUpdateGateway courseUpdateGateway;
    private final CourseDeleteGateway courseDeleteGateway;
    private final CourseBizConvertor courseConvertor;
    private final UserQueryGateway userQueryGateway;
    private final MsgResult msgResult;
    private  final MsgServiceImpl msgService;
    @CheckSemId
    @Override
    public List<List<Integer>> courseNum(Integer week, Integer semId) {
       return courseQueryGateway.getWeekCourses(semId,week);

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
    public void updateSingleCourse(Integer semId, UpdateSingleCourseCmd updateSingleCourseCmd,List<Integer> weekList,Boolean isBatchUpdate) {
        String userName =String.valueOf(StpUtil.getLoginId()) ;
        if(!isBatchUpdate){
            Map<String,Map<Integer,Integer>> map = courseUpdateGateway.updateSingleCourse(userName, semId, updateSingleCourseCmd);
            Optional<Integer> userId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId());
            for (Map.Entry<String, Map<Integer, Integer>> stringListEntry : map.entrySet()) {
                Map<String,Map<Integer,Integer>> map1=new HashMap<>();
                map1.put(stringListEntry.getKey(),stringListEntry.getValue());
                if(stringListEntry.getValue()==null){
                    msgResult.SendMsgToAll(map1,userId.orElseThrow(() -> new QueryException("请先登录")));
                }else if(!stringListEntry.getValue().isEmpty()){
                    msgResult.toNormalMsg(map1,userId.orElseThrow(() -> new QueryException("请先登录")));
                    stringListEntry.getValue().forEach((k,v)->msgService.deleteEvaMsg(k,null));
                }
            }
        }else{
            if(weekList==null)throw new UpdateException("请选择要修改的周数");
            Map<String,Map<Integer,Integer>> map=new HashMap<>();
            Map<Integer,Integer> ids = courseQueryGateway.selectCourInfoIds(updateSingleCourseCmd.getId(), weekList);
            for (Map.Entry<Integer, Integer> courseInfo : ids.entrySet()) {
                UpdateSingleCourseCmd updateBatchCourseCmd = new UpdateSingleCourseCmd()
                        .setId(courseInfo.getKey())
                        .setLocation(updateSingleCourseCmd.getLocation())
                        .setTime(updateSingleCourseCmd.getTime());
                updateBatchCourseCmd.getTime().setWeek(courseInfo.getValue());
                Map<String, Map<Integer, Integer>> stringMapMap = courseUpdateGateway.updateSingleCourse(userName, semId, updateBatchCourseCmd);
                //将stringMap中的数据添加到map中
                map.putAll(stringMapMap);
            }
            Optional<Integer> userId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId());
            int flag=0;
            for (Map.Entry<String, Map<Integer, Integer>> stringListEntry : map.entrySet()) {
                Map<String,Map<Integer,Integer>> map1=new HashMap<>();
                map1.put(stringListEntry.getKey(),stringListEntry.getValue());
                if(stringListEntry.getValue()==null){
                    if(flag==0){
                        msgResult.SendMsgToAll(map1,userId.orElseThrow(() -> new QueryException("请先登录")));
                        flag++;
                    }else{
                        continue;
                    }

                }else if(!stringListEntry.getValue().isEmpty()){
                    msgResult.toNormalMsg(map1,userId.orElseThrow(() -> new QueryException("请先登录")));
                    stringListEntry.getValue().forEach((k,v)->msgService.deleteEvaMsg(k,null));
                }
            }


        }

       /* Map<String,Map<Integer,Integer>> map = courseUpdateGateway.updateSingleCourse(userName, semId, updateSingleCourseCmd);
        Optional<Integer> userId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId());
        for (Map.Entry<String, Map<Integer, Integer>> stringListEntry : map.entrySet()) {
            Map<String,Map<Integer,Integer>> map1=new HashMap<>();
            map1.put(stringListEntry.getKey(),stringListEntry.getValue());
            if(stringListEntry.getValue()==null){
                msgResult.SendMsgToAll(map1,userId.orElseThrow(() -> new QueryException("请先登录")));
            }else if(!stringListEntry.getValue().isEmpty()){
                msgResult.toNormalMsg(map1,userId.orElseThrow(() -> new QueryException("请先登录")));
                stringListEntry.getValue().forEach((k,v)->msgService.deleteEvaMsg(k,null));
            }
        }*/
    }

    @CheckSemId
    @Override
    public void allocateTeacher(Integer semId, AlignTeacherCmd alignTeacherCmd) {
        Map<String, Map<Integer,Integer>> map = courseUpdateGateway.assignTeacher(semId, alignTeacherCmd);
        Optional<Integer> userId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId());
        msgResult.sendMsgtoTeacher(map, userId.orElseThrow(() -> new QueryException("请先登录")));
    }

    @CheckSemId
    @Override
    public void deleteCourses(Integer semId, Integer id, CoursePeriod coursePeriod) {
        Map<String, Map<Integer,Integer>> map = courseDeleteGateway.deleteCourses(semId, id, coursePeriod);
        Optional<Integer> userId = userQueryGateway.findIdByUsername((String) StpUtil.getLoginId());
        for (Map.Entry<String, Map<Integer, Integer>> stringMapEntry : map.entrySet()) {
            Map<String,Map<Integer,Integer>> map1=new HashMap<>();
            map1.put(stringMapEntry.getKey(),stringMapEntry.getValue());
            if(stringMapEntry.getValue()==null){
                msgResult.SendMsgToAll(map1, userId.orElseThrow(() -> new QueryException("请先登录")));
            }else if(!stringMapEntry.getValue().isEmpty()){
                msgResult.toNormalMsg(map1, userId.orElseThrow(() -> new QueryException("请先登录")));
                stringMapEntry.getValue().forEach((k,v)->msgService.deleteEvaMsg(k,null));
            }
        }

    }

    @Override
    @CheckSemId
    public void addExistCoursesDetails(Integer semId,Integer courseId, SelfTeachCourseTimeCO timeCO) {
            //判断该老师是否有课程
            courseUpdateGateway.judgeHasCourseOrEva(courseId,timeCO);
            courseUpdateGateway.addExistCoursesDetails(semId,courseId,timeCO);
    }

    @CheckSemId
    @Override
    public void addNotExistCoursesDetails(Integer semId, Integer teacherId, UpdateCourseCmd courseInfo, List<SelfTeachCourseTimeCO> dateArr) {
        courseUpdateGateway.addNotExistCoursesDetails(semId,teacherId,courseInfo,dateArr);
    }
}
