package edu.cuit.app.service.operate.course.query;

import edu.cuit.app.convertor.course.CourseBizConvertor;

import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.course.CourseModelCO;
import edu.cuit.client.dto.data.course.CoursePeriod;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserCourseDetailQueryExec {

    private final CourseQueryGateway courseQueryGateway;

    private final CourseBizConvertor courseConvertor;
    public  CourseDetailCO getUserCourseDetail(List<SingleCourseEntity> singleCourseEntities,Integer semId){
        List<CourseType> typeList=null;
        CourseModelCO courseModelCO=null;
        if(!singleCourseEntities.isEmpty()){
             typeList = courseQueryGateway.getCourseType(singleCourseEntities.get(0).getCourseEntity().getId());
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
}
