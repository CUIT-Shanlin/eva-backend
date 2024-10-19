package edu.cuit.infra.gateway.impl.course;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.client.bo.CourseExcelBO;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import edu.cuit.client.dto.clientobject.course.SubjectCO;
import edu.cuit.client.dto.cmd.course.AlignTeacherCmd;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import edu.cuit.client.dto.cmd.course.UpdateCoursesCmd;
import edu.cuit.client.dto.cmd.course.UpdateSingleCourseCmd;
import edu.cuit.client.dto.data.Term;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import edu.cuit.infra.convertor.course.CourseConvertor;
import edu.cuit.infra.dal.database.dataobject.course.*;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.*;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.gateway.impl.course.operate.CourseImportExce;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class CourseUpdateGatewayImpl implements CourseUpdateGateway {
    private final CourseConvertor courseConvertor;
    private final CourInfMapper courInfMapper;
    private final SemesterMapper semesterMapper;
    private final CourseMapper courseMapper;
    private final CourseTypeCourseMapper courseTypeCourseMapper;
    private final CourseTypeMapper courseTypeMapper;
    private final SubjectMapper subjectMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final SysUserMapper userMapper;
    private final CourseImportExce courseImportExce;

    @Override
    @Transactional
    public String updateCourse(Integer semId, UpdateCourseCmd updateCourseCmd) {
        List<Integer> courseIdList=new ArrayList<>();
        if(updateCourseCmd.getIsUpdate()){
            //先查出课程表中的subjectId
            Integer subjectId = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", updateCourseCmd.getId())).getSubjectId();
            //再根据subjectId更新对应科目表
            subjectMapper.update(courseConvertor.toSubjectDO(updateCourseCmd.getSubjectMsg()),new QueryWrapper<SubjectDO>().eq("id",subjectId));
            //根据subjectId来找出所有课程Id集合
            QueryWrapper<CourseDO> wrapper = new QueryWrapper<CourseDO>().eq("subject_id", subjectId);
            if(semId!=null){
                wrapper.eq("semester_id",semId);
            }
            courseIdList = courseMapper.selectList(wrapper).stream().map(CourseDO::getId).toList();
        }else{
            courseIdList.add(updateCourseCmd.getId());
        }
        List<Integer> typeIds = courseTypeCourseMapper.selectList(new QueryWrapper<CourseTypeCourseDO>().eq("course_id", updateCourseCmd.getId())).stream().map(CourseTypeCourseDO::getTypeId).toList();
        //判断typeIds是否与typeIdList一致
        boolean isEq = !typeIds.equals(updateCourseCmd.getTypeIdList());
        //更新课程表的templateId字段
        CourseDO courseDO = new CourseDO();
        courseDO.setTemplateId(updateCourseCmd.getTemplateId());
        for (Integer i : courseIdList) {
            if(isEq){
                //先删除，再添加
                courseTypeCourseMapper.delete(new QueryWrapper<CourseTypeCourseDO>().eq("course_id",i));
                //更新课程类型快照表
                for (Integer typeId : updateCourseCmd.getTypeIdList()) {
                    CourseTypeCourseDO courseTypeCourseDO = new CourseTypeCourseDO();
                    courseTypeCourseDO.setCourseId(updateCourseCmd.getId());
                    courseTypeCourseDO.setTypeId(typeId);
                    courseTypeCourseDO.setCreateTime(LocalDateTime.now());
                    courseTypeCourseDO.setUpdateTime(LocalDateTime.now());
                    courseTypeCourseMapper.insert(courseTypeCourseDO);
                }
            }
            //更新课程表的templateId字段
            courseMapper.update(courseDO,new QueryWrapper<CourseDO>().eq("id",i));


        }
        return updateCourseCmd.getSubjectMsg().getName()+"课程的信息被修改了";

    }

    @Override
    @Transactional
    public void updateCourses(Integer semId, UpdateCoursesCmd updateCoursesCmd) {
        //修改updateCoursesCmd中的courseIdList集合中id对应的课程的templateId
        List<Integer> courseIdList = updateCoursesCmd.getCourseIdList();
        for (Integer i : courseIdList) {
            CourseDO courseDO = new CourseDO();
            courseDO.setTemplateId(updateCoursesCmd.getTemplateId());
            courseMapper.update(courseDO,new QueryWrapper<CourseDO>().eq("id",i).eq("semester_id",semId));
        }

    }

    @Override
    @Transactional
    public Map<String,List<Integer>> updateSingleCourse(String userName,Integer semId, UpdateSingleCourseCmd updateSingleCourseCmd) {
        //先将要修改的那节课查出来
        CourInfDO courINfo = courInfMapper.selectById(updateSingleCourseCmd.getId());
        CourseDO courseDo=null;
        if(courINfo==null){
            throw new QueryException("该节课不存在");
        }

        //先根据用户名来查出教师id
        Integer teacherId = userMapper.selectOne(new QueryWrapper<SysUserDO>().eq("username", userName)).getId();
        //根据teacherId和semId找出他的所有授课
        List<CourseDO> courseDOList = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", teacherId).eq("semester_id", semId));
        //根据CourseDO中的Id和updateSingleCourseCmd中的time来判断来查看这个老师在这个时间段是否有课程
        for (CourseDO courseDO : courseDOList) {
            CourInfDO courInfDO = courInfMapper.selectOne(new QueryWrapper<CourInfDO>()
                    .eq("course_id", courseDO.getId())
                    .eq("week", updateSingleCourseCmd.getTime().getWeek())
                    .eq("day", updateSingleCourseCmd.getTime().getDay())
                    .le("start_time", updateSingleCourseCmd.getTime().getEndTime())
                    .ge("end_time", updateSingleCourseCmd.getTime().getStartTime()));
            if (courInfDO != null) {
                throw new UpdateException("该时间段已有课程");
            }
            if(courseDO.getId()==courINfo.getCourseId()){
                courseDo=courseDO;
            }
        }
        //判断location是否被占用
            //先根据updateSingleCourseCmd中的数据找出所有对应时间段的课程
                List<CourInfDO> courInfDOList = courInfMapper.selectList(new QueryWrapper<CourInfDO>()
                        .eq("week", updateSingleCourseCmd.getTime().getWeek())
                        .eq("day", updateSingleCourseCmd.getTime().getDay())
                        .le("start_time", updateSingleCourseCmd.getTime().getEndTime())
                        .ge("end_time", updateSingleCourseCmd.getTime().getStartTime()));
                for (CourInfDO courInfDO : courInfDOList) {
                    if(courseMapper.selectById(courInfDO.getCourseId()).getSemesterId().equals(semId)){
                        if(courInfDO.getLocation().equals(updateSingleCourseCmd.getLocation())){
                            throw new UpdateException("该时间段该地点已有课程");
                        }
                    }
                }
        String name = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", courseDo.getSubjectId())).getName();
        //更新一节课的数据
        CourInfDO courInfDO = courseConvertor.toCourInfDO(updateSingleCourseCmd);
        courInfDO.setUpdateTime(LocalDateTime.now());
        courInfDO.setLocation(updateSingleCourseCmd.getLocation());
        courInfMapper.update(courInfDO,new QueryWrapper<CourInfDO>().eq("id",updateSingleCourseCmd.getId()));
        //超出所有要评教这节课的老师id
        List<Integer> userList = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courINfo.getCourseId())).stream().map(EvaTaskDO::getTeacherId).toList();
        Map<String,List<Integer>> map=new HashMap<>();
        map.put("你所评教的"+name+"课程第"+courINfo.getWeek()+"周，星期"
                +courINfo.getDay()+"，第"+courINfo.getStartTime()+"-"+courINfo.getEndTime()+"节，上课时间已修改为第"
                +updateSingleCourseCmd.getTime().getWeek()+"周，星期"+updateSingleCourseCmd.getTime().getDay()
                +"，第"+updateSingleCourseCmd.getTime().getStartTime()+"-"+updateSingleCourseCmd.getTime().getEndTime()+"节。教室："+
                updateSingleCourseCmd.getLocation(),userList);
        return map;
    }

    @Override
    @Transactional
    public Void updateCourseType(CourseType courseType) {
        //根据id更新课程类型
        courseTypeMapper.update(courseConvertor.toCourseTypeDO(courseType),new QueryWrapper<CourseTypeDO>().eq("id",courseType.getId()));
        return null;
    }

    @Override
    @Transactional
    public Void addCourseType(CourseType courseType) {
        // 根据课程类型名称查询数据库
        CourseTypeDO existingCourseType = courseTypeMapper.selectOne(new QueryWrapper<CourseTypeDO>().eq("name", courseType.getName()));

        if (existingCourseType == null) {
            // 如果不存在相同名称的课程类型，则插入新记录
            courseTypeMapper.insert(courseConvertor.toCourseTypeDO(courseType));
        }else {
            throw new UpdateException("该课程类型已存在");
        }

        return null;
    }

    @Override
    @Transactional
    public Void addCourse(Integer semId) {
        //TODO（接口已删除）
        return null;
    }

    @Override
    @Transactional
    public Map<String,List<Integer>> assignTeacher(Integer semId, AlignTeacherCmd alignTeacherCmd) {

        Integer courseId=alignTeacherCmd.getId();
        CourInfDO courInfDO = courInfMapper.selectById(courseId);
        if(courInfDO==null){
            throw new QueryException("该节课不存在");
        }
        //查看评教老师在那个时间段是否已经有评教任务
            judgeAlsoHasTask(alignTeacherCmd.getEvaTeacherIdList(),courInfDO);
        //判断评教老师再改时间段是否已经有课了
            judgeAlsoHasCourse(semId,alignTeacherCmd.getEvaTeacherIdList(),courInfDO);
        //遍历并创建评教任务对象，在插入评教任务表
        List<EvaTaskDO> taskList = alignTeacherCmd.getEvaTeacherIdList().stream().map(id -> {
            EvaTaskDO evaTaskDO = new EvaTaskDO();
            evaTaskDO.setTeacherId(id);
            evaTaskDO.setCourInfId(courseId);
            evaTaskDO.setStatus(0);
            evaTaskDO.setCreateTime(LocalDateTime.now());
            evaTaskDO.setUpdateTime(LocalDateTime.now());
            evaTaskDO.setIsDeleted(0);
            return evaTaskDO;
        }).toList();
        taskList.forEach(evaTaskMapper::insert);
        Integer subjectId = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", courseId)).getSubjectId();
        String name = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", subjectId)).getName();
        Map<String,List<Integer>> map=new HashMap<>();
        map.put("你已经被分配去听第"+courInfDO.getWeek()+"周，星期"
                +courInfDO.getDay()+"，第"+courInfDO.getStartTime()+"-"+courInfDO.getEndTime()+"节，"+name+"课程。位置："+courInfDO.getLocation()
                ,alignTeacherCmd.getEvaTeacherIdList());
        return map;
    }

    private void judgeAlsoHasCourse(Integer semId,List<Integer> evaTeacherIdList, CourInfDO courInfDO) {
        List<Integer> list = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semId).in("teacher_id", evaTeacherIdList)).stream().map(CourseDO::getId).toList();
       CourInfDO courInfDOList = courInfMapper.selectOne(new QueryWrapper<CourInfDO>()
                .eq("week", courInfDO.getWeek())
                .eq("day", courInfDO.getDay())
                .le("start_time", courInfDO.getEndTime())
                .ge("end_time", courInfDO.getStartTime())
                .in("course_id", list));
        if(courInfDOList!=null){
            throw new UpdateException("该时间段已有课程");
        }
    }

    private void judgeAlsoHasTask(List<Integer> userList,CourInfDO courInfDO){
        List<Integer> courInfoList = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("teacher_id", userList)).stream().map(EvaTaskDO::getCourInfId).toList();
        CourInfDO courINfo = courInfMapper.selectOne(new QueryWrapper<CourInfDO>()
                .eq("week", courInfDO.getWeek())
                .eq("day", courInfDO.getDay())
                .le("start_time", courInfDO.getEndTime())
                .ge("end_time", courInfDO.getStartTime())
                .in("id", courInfoList));
        if(courINfo!=null){
            throw new UpdateException("课程时间冲突，评教老师在该时间段已经有了评教任务");
        }

    }

    @Override
    @Transactional
    public Void importCourseFile(Map<String, List<CourseExcelBO>> courseExce, SemesterCO semester, Integer type) {

        if(semester.getId() != null && semesterMapper.exists(new QueryWrapper<SemesterDO>().eq("id",semester.getId()))){
            //执行已有学期的删除添加逻辑
            courseImportExce.deleteCourse(semester.getId());
        }else{
            //直接插入学期
            semesterMapper.insert(courseConvertor.toSemesterDO(semester));
        }
        courseImportExce.addAll(courseExce, type,semester.getId());
        return null;
    }

    @Override
    @Transactional
    public Map<String,Map<Integer,Integer>> updateSelfCourse(String userName, SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeCO> timeList) {
        String msg=null;
        Integer userId = userMapper.selectOne(new QueryWrapper<SysUserDO>().eq("username", userName)).getId();
        if(userId==null){
            throw new QueryException("用户不存在");
        }
        CourseDO courseDO = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", selfTeachCourseCO.getId()).eq("teacher_id", userId));
        if(courseDO==null){
            //课程不存在(抛出异常)
            throw new QueryException("用户对应课程不存在");
        }
        List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", userId).eq("semester_id", courseDO.getSemesterId()));
        courseDOS.removeIf(aDo -> aDo.getId().equals(selfTeachCourseCO.getId()));
        SubjectDO subjectDO = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", courseDO.getSubjectId()));
        msg=toJudge(subjectDO,selfTeachCourseCO);
        //课程类型
        msg+=JudgeCourseType(courseDO,selfTeachCourseCO);
        //课程时间段
        Map<Integer,Integer> taskMap=new HashMap<>();
        msg+=JudgeCourseTime(courseDO,timeList,courseDOS,selfTeachCourseCO,taskMap);
        return null;
    }

    private String JudgeCourseTime(CourseDO courseDO, List<SelfTeachCourseTimeCO> timeList,List<CourseDO> courseDOList,SelfTeachCourseCO selfTeachCourseCO,Map<Integer,Integer> taskMap) {
        String msg="";
        Stream<Integer> courInfoIds = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id", courseDO.getTeacherId())).stream().map(EvaTaskDO::getCourInfId);
//        List<CourInfDO> evaInfDOList = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("id", courInfoIds));
        List<CourInfDO> courInfoList = courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id", courseDO.getId()));
        List<CourInfDO> courseChangeList=new ArrayList<>();
        for (SelfTeachCourseTimeCO selfTeachCourseTimeCO : timeList) {
            for (Integer week : selfTeachCourseTimeCO.getWeeks()) {
               CourInfDO courInfDO=new CourInfDO();
               courInfDO.setCourseId(courseDO.getId());
               courInfDO.setWeek(week);
               courInfDO.setDay(selfTeachCourseTimeCO.getDay());
               courInfDO.setStartTime(selfTeachCourseTimeCO.getStartTime());
               courInfDO.setEndTime(selfTeachCourseTimeCO.getEndTime());
               courInfDO.setLocation(selfTeachCourseTimeCO.getClassroom());
               courseChangeList.add(courInfDO);
            }
        }
        List<CourInfDO> difference = getDifference(courseChangeList, courInfoList);
        List<CourInfDO> difference2 = getDifference( courInfoList,courseChangeList);
        if(difference.isEmpty()){
            return "";
        }else{
            for (CourInfDO courInfDO : difference2) {
                courInfMapper.delete(new QueryWrapper<CourInfDO>()
                        .eq("course_id", courInfDO.getCourseId())
                        .eq("week", courInfDO.getWeek()).eq("day", courInfDO.getDay())
                        .eq("start_time", courInfDO.getStartTime()).eq("end_time",courInfDO.getEndTime()));
                evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courInfDO.getId())).forEach(evaTaskDO -> taskMap.put(evaTaskDO.getId(),evaTaskDO.getTeacherId()));
                evaTaskMapper.delete(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courInfDO.getId()));
            }
            for (CourInfDO courInfDO : difference) {
                for(CourseDO course : courseDOList){
                    QueryWrapper<CourInfDO> wrapper = new QueryWrapper<CourInfDO>()
                            .eq("week", courInfDO.getWeek())
                            .eq("day", courInfDO.getDay())
                            .le("start_time", courInfDO.getEndTime())
                            .ge("end_time", courInfDO.getStartTime())
                            .eq("course_id", course.getId());
                    //判断对应时间段是否已经有课了
                    if(courInfMapper.selectOne(wrapper)!=null){
                        throw new UpdateException("该时间段你已经有课了");
                    }
                }
                //评教
                QueryWrapper<CourInfDO> wrapper = new QueryWrapper<CourInfDO>()
                        .eq("week", courInfDO.getWeek())
                        .eq("day", courInfDO.getDay())
                        .le("start_time", courInfDO.getEndTime())
                        .ge("end_time", courInfDO.getStartTime())
                        .in("course_id", courInfoIds);
                    if(courInfMapper.selectOne(wrapper)!=null){
                        throw new UpdateException("该时间段你有要去评教的课程");
                    }
                //判断对应时间段的教室是否被占用
                wrapper.eq("location", courInfDO.getLocation());
                if(courInfMapper.selectOne(wrapper)!=null){
                    //被占用了，抛出异常
                    throw new UpdateException("该时间段教室已占用");
                }
                courInfMapper.insert(courInfDO);
            }
            return msg+selfTeachCourseCO.getName()+"课程的上课时间被修改了,"+"因而取消您对该课程的评教任务";
        }
    }
    public  List<CourInfDO> getDifference(List<CourInfDO> courseChangeList, List<CourInfDO> courInfoList) {
        return courseChangeList.stream()
                .filter(courseChange -> courInfoList.stream()
                        .noneMatch(courInfo -> Objects.equals(courseChange.getWeek(), courInfo.getWeek())
                                && Objects.equals(courseChange.getDay(), courInfo.getDay())
                                && Objects.equals(courseChange.getStartTime(), courInfo.getStartTime())
                                && Objects.equals(courseChange.getEndTime(), courInfo.getEndTime())))
                .collect(Collectors.toList());
    }

    private String JudgeCourseType(CourseDO courseDO,SelfTeachCourseCO selfTeachCourseCO) {
        String msg="";
        // 获取课程类型ID
        List<Integer> typeIdDo = courseTypeCourseMapper.selectList(new QueryWrapper<CourseTypeCourseDO>().eq("course_id", courseDO.getId()))
                .stream()
                .map(CourseTypeCourseDO::getTypeId)
                .sorted()
                .collect(Collectors.toList());

        // 获取自定义课程类型的名称
        List<String> typeName = selfTeachCourseCO.getTypeList().stream()
                .map(CourseType::getName)
                .collect(Collectors.toList());

        // 获取数据库中的类型ID
        List<Integer> typeIdIn = courseTypeMapper.selectList(new QueryWrapper<CourseTypeDO>().in("name", typeName))
                .stream()
                .map(CourseTypeDO::getId)
                .sorted()
                .toList();

        // 比较两个集合
        if (typeIdIn.equals(typeIdDo)) {
            return msg;
        } else {
            courseTypeCourseMapper.delete(new QueryWrapper<CourseTypeCourseDO>().eq("course_id",courseDO.getId() ).in("type_id", typeIdDo));
            typeIdIn.forEach(typeId -> {
                CourseTypeCourseDO courseTypeCourseDO=new CourseTypeCourseDO();
                courseTypeCourseDO.setCourseId(courseDO.getId());
                courseTypeCourseDO.setTypeId(typeId);
                courseTypeCourseDO.setCreateTime(LocalDateTime.now());
                courseTypeCourseDO.setUpdateTime(LocalDateTime.now());
                courseTypeCourseMapper.insert(courseTypeCourseDO);
            });

            return msg+"课程类型被修改为:"+String.join(",", typeName)+"。";
        }

    }

    private String toJudge(SubjectDO subjectDO, SelfTeachCourseCO selfTeachCourseCO) {
        String msg="";
        String name = subjectDO.getName();
        //0: 理论课相关默认；1: 实验课相关默认；
        String natureExp = subjectDO.getNature().equals(0) ? "理论课" : "实践课";
        if(!subjectDO.getName().equals(selfTeachCourseCO.getName())){
            subjectDO.setName(selfTeachCourseCO.getName());
            if(!subjectDO.getNature().equals(selfTeachCourseCO.getNature())){
                subjectDO.setNature(selfTeachCourseCO.getNature());
                return msg+name+"课程的名称被改成了"+subjectDO.getName()+"，类型被改成了"+natureExp+"。";
            }
            return msg+name+"课程的名称被改成了"+subjectDO.getName()+"。";
        }
        return "";
    }

    @Override
    @Transactional
    public Void addExistCoursesDetails(Integer courseId, SelfTeachCourseTimeCO timeCO) {
        for (Integer week : timeCO.getWeeks()) {
            judgeAlsoHasLocation(week,timeCO);
            CourInfDO courInfDO=new CourInfDO();
            courInfDO.setCourseId(courseId);
            courInfDO.setWeek(week);
            courInfDO.setDay(timeCO.getDay());
            courInfDO.setStartTime(timeCO.getStartTime());
            courInfDO.setEndTime(timeCO.getEndTime());
            courInfDO.setLocation(timeCO.getClassroom());
            courInfDO.setCreateTime(LocalDateTime.now());
            courInfDO.setUpdateTime(LocalDateTime.now());
            courInfMapper.insert(courInfDO);
        }


        return null;
    }

    private void judgeAlsoHasLocation(Integer week, SelfTeachCourseTimeCO timeCO) {
        CourInfDO courInfDO = courInfMapper.selectOne(new QueryWrapper<CourInfDO>()
                .eq("week", week)
                .eq("day", timeCO.getDay())
                .eq("location", timeCO.getClassroom())
                .le("start_time", timeCO.getEndTime())
                .ge("end_time", timeCO.getStartTime()));
        if(courInfDO!=null){
            throw new UpdateException("该时间段教室冲突，请修改时间");
        }

    }

    @Override
    @Transactional
    public void addNotExistCoursesDetails(Integer semId, Integer teacherId, UpdateCourseCmd courseInfo, List<SelfTeachCourseTimeCO> dateArr) {
        SubjectDO subjectDO1 = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("name", courseInfo.getSubjectMsg().getName()).eq("nature", courseInfo.getSubjectMsg().getNature()));
        Integer subjectId=null;
        if(subjectDO1==null) {
            SubjectDO subjectDO = courseConvertor.toSubjectDO(courseInfo.getSubjectMsg());
            //向subject表插入数据并返回主键ID
            subjectMapper.insert(subjectDO);
            subjectId=subjectDO.getId();
        }else {
            subjectId=subjectDO1.getId();
        }
        //向course表中插入数据
        CourseDO courseDO = courseConvertor.toCourseDO(courseInfo, subjectId, teacherId, semId);
        courseMapper.insert(courseDO);
        //再根据teacherId和subjectId又将他查出来
       Integer courseDOId = courseDO.getId();
        //插入课程类型
        for (Integer i : courseInfo.getTypeIdList()) {
            CourseTypeCourseDO courseTypeCourseDO = new CourseTypeCourseDO();
            courseTypeCourseDO.setCourseId(courseDOId);
            courseTypeCourseDO.setTypeId(i);
            courseTypeCourseDO.setCreateTime(courseInfo.getCreateTime());
            courseTypeCourseDO.setUpdateTime(courseInfo.getUpdateTime());
            courseTypeCourseMapper.insert(courseTypeCourseDO);
        }
        //插入课程时间表
        for (SelfTeachCourseTimeCO time : dateArr) {
            for (Integer week : time.getWeeks()) {
                judgeAlsoHasLocation(week,time);
                CourInfDO courInfDO = new CourInfDO();
                courInfDO.setCourseId(courseDOId);
                courInfDO.setWeek(week);
                courInfDO.setDay(time.getDay());
                courInfDO.setStartTime(time.getStartTime());
                courInfDO.setEndTime(time.getEndTime());
                courInfDO.setLocation(time.getClassroom());
                courInfDO.setCreateTime(courseInfo.getCreateTime());
                courInfDO.setUpdateTime(courseInfo.getCreateTime());
                courInfMapper.insert(courInfDO);
            }
        }

    }

    @Override
    public Boolean isImported(Integer type, Term term) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(term.getStartDate(), formatter);
        SemesterDO semesterDO = semesterMapper.selectOne(new QueryWrapper<SemesterDO>().eq("start_date", date).eq("start_year", term.getStartYear()).eq("end_year", term.getEndYear()));
        if(semesterDO==null)return false;
        List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("sem_id", semesterDO.getId()));
        if(courseDOS==null)return false;
        List<SubjectDO> subjectDOS = subjectMapper.selectList(new QueryWrapper<SubjectDO>().in("id", courseDOS.stream().map(CourseDO::getSubjectId).toList()));
        for (SubjectDO subjectDO : subjectDOS) {
            if(!subjectDO.getNature().equals(type)){
                return false;
            }
        }


        return true;
    }
}
