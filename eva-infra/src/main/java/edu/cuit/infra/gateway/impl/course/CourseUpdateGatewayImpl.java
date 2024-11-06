package edu.cuit.infra.gateway.impl.course;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.client.bo.CourseExcelBO;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeInfoCO;
import edu.cuit.client.dto.cmd.course.*;
import edu.cuit.client.dto.data.Term;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import edu.cuit.infra.convertor.course.CourseConvertor;
import edu.cuit.infra.dal.database.dataobject.course.*;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.*;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.enums.cache.ClassroomCacheConstants;
import edu.cuit.infra.enums.cache.CourseCacheConstants;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.infra.gateway.impl.course.operate.CourseImportExce;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private final LocalCacheManager localCacheManager;
    private final CourseCacheConstants courseCacheConstants;
    private final FormTemplateMapper formTemplateMapper;
    private final EvaCacheConstants evaCacheConstants;
    private final ClassroomCacheConstants classroomCacheConstants;


    /**
     * 修改一门课程
     *@param semId 学期id
     *@param updateCourseCmd 修改课程信息
     *
     * */
    @Override
    @Transactional
    public Map<String,Map<Integer,Integer>> updateCourse(Integer semId, UpdateCourseCmd updateCourseCmd) {
        List<Integer> courseIdList=new ArrayList<>();
        //先查出课程表中的subjectId
        CourseDO courseDO = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", updateCourseCmd.getId()));
        if(courseDO==null){
            throw new QueryException("没有该课程");
        }
        SysUserDO userDO = userMapper.selectById(courseDO.getTeacherId());
        if(updateCourseCmd.getIsUpdate()){
            Integer subjectId = courseDO.getSubjectId();
            //再根据subjectId更新对应科目表
            subjectMapper.update(courseConvertor.toSubjectDO(updateCourseCmd.getSubjectMsg()),new QueryWrapper<SubjectDO>().eq("id",subjectId));
            courseIdList.add(courseDO.getId());
            localCacheManager.invalidateCache(null,courseCacheConstants.SUBJECT_LIST);
        }else{
            courseIdList.add(updateCourseCmd.getId());
            SubjectDO subjectDO = subjectMapper.selectById(courseDO.getSubjectId());
            if(!subjectDO.getName().equals(updateCourseCmd.getSubjectMsg().getName())|| !Objects.equals(subjectDO.getNature(), updateCourseCmd.getSubjectMsg().getNature())){
                    SubjectDO sujectDo=new SubjectDO();
                    sujectDo.setName(updateCourseCmd.getSubjectMsg().getName());
                    sujectDo.setNature(updateCourseCmd.getSubjectMsg().getNature());
                    sujectDo.setUpdateTime(LocalDateTime.now());
                    sujectDo.setCreateTime(LocalDateTime.now());
                    subjectMapper.insert(sujectDo);
                    courseDO.setSubjectId(sujectDo.getId());
                    //查看是否要删除subject
                if(courseMapper.selectCount(new QueryWrapper<CourseDO>().eq("subject_id",subjectDO.getId()))==1){
                    subjectMapper.delete(new QueryWrapper<SubjectDO>().eq("id",subjectDO.getId()));
                }
                localCacheManager.invalidateCache(null,courseCacheConstants.SUBJECT_LIST);
            }

        }
        List<Integer> typeIds = courseTypeCourseMapper.selectList(new QueryWrapper<CourseTypeCourseDO>().eq("course_id", updateCourseCmd.getId())).stream().map(CourseTypeCourseDO::getTypeId).toList();
        //判断typeIds是否与typeIdList一致
        boolean isEq = !typeIds.equals(updateCourseCmd.getTypeIdList());
        //更新课程表的templateId字段
        CourseDO courseDO1 = new CourseDO();
        courseDO1.setTemplateId(updateCourseCmd.getTemplateId());
        courseDO1.setSubjectId(courseDO.getSubjectId());
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
            courseMapper.update(courseDO1,new QueryWrapper<CourseDO>().eq("id",i));
        }
        /*List<Integer> list = courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id", updateCourseCmd.getId())).stream().map(CourInfDO::getId).toList();

        List<Integer> list1 = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id", list).eq("status", 0)).stream().map(EvaTaskDO::getTeacherId).toList();*/
       Map<String,Map<Integer,Integer>> map=new HashMap<>();
       map.put( userDO.getName()+"的"+updateCourseCmd.getSubjectMsg().getName()+"课程的信息被修改了",null);
       //清缓存
        localCacheManager.invalidateCache(courseCacheConstants.COURSE_LIST_BY_SEM, String.valueOf(semId));
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId));
        localCacheManager.invalidateCache(null,classroomCacheConstants.ALL_CLASSROOM);
        return map;

    }


    /**
     * 批量修改课程的模板
     *@param semId 学期id
     *  @param updateCoursesCmd 批量修改课程信息
     *
     * */
    @Override
    @Transactional
    public void updateCourses(Integer semId, UpdateCoursesCmd updateCoursesCmd) {
        //修改updateCoursesCmd中的courseIdList集合中id对应的课程的templateId
        List<Integer> courseIdList = updateCoursesCmd.getCourseIdList();
        for (Integer i : courseIdList) {
            CourseDO courseDO = new CourseDO();
            courseDO.setTemplateId(updateCoursesCmd.getTemplateId());
            courseMapper.update(courseDO,new QueryWrapper<CourseDO>().eq("id",i).eq("semester_id",semId));
            CourseDO courseDO1 = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", i).eq("semester_id", semId));
            if(courseDO1==null) throw new QueryException("并未找到相关课程");
            Integer subjectId =courseDO1 .getSubjectId();
            String name = subjectMapper.selectById(subjectId).getName();
            LogUtils.logContent(name+"(ID:"+i+")课程模板");
        }
        localCacheManager.invalidateCache(courseCacheConstants.COURSE_LIST_BY_SEM, String.valueOf(semId));
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId));

    }

    @Override
    @Transactional
    public Map<String,Map<Integer,Integer>> updateSingleCourse(String userName,Integer semId, UpdateSingleCourseCmd updateSingleCourseCmd) {
        if(updateSingleCourseCmd.getTime()==null){
            throw new UpdateException("修改的时间不能为空");
        }
        //先将要修改的那节课查出来
        CourInfDO courINfo = courInfMapper.selectById(updateSingleCourseCmd.getId());
        CourseDO courseDo=null;
        if(courINfo==null){
            throw new QueryException("该节课不存在");
        }
        //先根据用户名来查出教师id
        SysUserDO userDO = userMapper.selectById(courseMapper.selectById(courINfo.getCourseId()).getTeacherId());
        if(userDO==null)throw new QueryException("老师不存在");
        Integer teacherId = userDO.getId();
        //根据teacherId和semId找出他的所有授课
        List<CourseDO> courseDOList = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", teacherId).eq("semester_id", semId));
        //根据CourseDO中的Id和updateSingleCourseCmd中的time来判断来查看这个老师在这个时间段是否有课程
        for (CourseDO courseDO : courseDOList) {
            if(courseDO.getId().equals(courINfo.getCourseId())){
                courseDo=courseDO;
                continue;
            }
            CourInfDO courInfDO = courInfMapper.selectOne(new QueryWrapper<CourInfDO>()
                    .eq("course_id", courseDO.getId())
                    .eq("week", updateSingleCourseCmd.getTime().getWeek())
                    .eq("day", updateSingleCourseCmd.getTime().getDay())
                    .le("start_time", updateSingleCourseCmd.getTime().getEndTime())
                    .ge("end_time", updateSingleCourseCmd.getTime().getStartTime()));
            if (courInfDO != null) {
                throw new UpdateException("该时间段已有课程");
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
        assert courseDo != null;
        SubjectDO subjectDO = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", courseDo.getSubjectId()));
        String name = subjectDO.getName();
        //更新一节课的数据
        CourInfDO courInfDO = courseConvertor.toCourInfDO(updateSingleCourseCmd);
        courInfDO.setUpdateTime(LocalDateTime.now());
        courInfDO.setLocation(updateSingleCourseCmd.getLocation());
        courInfMapper.update(courInfDO,new QueryWrapper<CourInfDO>().eq("id",updateSingleCourseCmd.getId()));
        //找出所有要评教这节课的老师id
        List<EvaTaskDO> taskDOList = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courINfo.getCourseId()).eq("status",0));
        Map<Integer,Integer> mapEva=new HashMap<>();
        for (EvaTaskDO i : taskDOList) {
            EvaTaskDO evaTaskDO = new EvaTaskDO();
            evaTaskDO.setStatus(2);
            evaTaskMapper.update(evaTaskDO,new QueryWrapper<EvaTaskDO>().eq("teacher_id",i.getTeacherId()).eq("cour_inf_id",courINfo.getCourseId()));
            mapEva.put(i.getTeacherId(),i.getId());
        }
        Map<String,Map<Integer,Integer>> map=new HashMap<>();
 /*       "你所评教的"+name+"课程第"+courINfo.getWeek()+"周，星期"
                +courINfo.getDay()+"，第"+courINfo.getStartTime()+"-"+courINfo.getEndTime()+"节，上课时间已修改为第"
                +updateSingleCourseCmd.getTime().getWeek()+"周，星期"+updateSingleCourseCmd.getTime().getDay()
                +"，第"+updateSingleCourseCmd.getTime().getStartTime()+"-"+updateSingleCourseCmd.getTime().getEndTime()+"节。教室："+
                updateSingleCourseCmd.getLocation()*/
        map.put(name+"课程的上课时间被修改了",null);
        map.put("因为"+name+"课程的上课时间修改，故已取消您对该课程的评教任务",mapEva);
        LogUtils.logContent(name+"上课时间信息");
        localCacheManager.invalidateCache(null,evaCacheConstants.LOG_LIST);
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM,String.valueOf(semId));
        localCacheManager.invalidateCache(null,classroomCacheConstants.ALL_CLASSROOM);
        return map;
    }

    @Override
    @Transactional
    public Void updateCourseType(UpdateCourseTypeCmd courseType) {
        //判断课程类型是否存在
        CourseTypeDO courseTypeDO = courseTypeMapper.selectById(courseType.getId());
        if(courseTypeDO==null)throw new QueryException("该课程类型不存在");
        //根据id更新课程类型
        courseTypeDO.setName(courseType.getName());
        courseTypeDO.setDescription(courseType.getDescription());
        courseTypeMapper.update(courseTypeDO,new QueryWrapper<CourseTypeDO>().eq("id",courseType.getId()));
        LogUtils.logContent(courseType.getName()+"课程类型");
        localCacheManager.invalidateCache(null,courseCacheConstants.COURSE_TYPE_LIST);
        return null;
    }

    @Override
    @Transactional
    public Void addCourseType(CourseType courseType) {
        // 根据课程类型名称查询数据库
        CourseTypeDO existingCourseType = courseTypeMapper.selectOne(new QueryWrapper<CourseTypeDO>().eq("name", courseType.getName()));
        if (existingCourseType == null) {
            // 如果不存在相同名称的课程类型，则插入新记录
            CourseTypeDO courseTypeDO=new CourseTypeDO();
            courseTypeDO.setName(courseType.getName());
            courseTypeDO.setDescription(courseType.getDescription());
            courseTypeMapper.insert(courseTypeDO);
            localCacheManager.invalidateCache(null,courseCacheConstants.COURSE_TYPE_LIST);
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
    public Map<String,Map<Integer,Integer>> assignTeacher(Integer semId, AlignTeacherCmd alignTeacherCmd) {

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
            return evaTaskDO;
        }).toList();
        taskList.forEach(evaTaskMapper::insert);
        Map<Integer,Integer> mapTask=new HashMap<>();
        taskList.forEach(evaTaskDO -> mapTask.put(evaTaskDO.getId(),evaTaskDO.getTeacherId()));
        Integer subjectId = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", courInfDO.getCourseId())).getSubjectId();
        String name = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", subjectId)).getName();
        Map<String,Map<Integer,Integer>> map=new HashMap<>();
        map.put("你已经被分配去听第"+courInfDO.getWeek()+"周，星期"
                +courInfDO.getDay()+"，第"+courInfDO.getStartTime()+"-"+courInfDO.getEndTime()+"节，"+name+"课程。位置："+courInfDO.getLocation()
                ,mapTask);
        for (Integer i : alignTeacherCmd.getEvaTeacherIdList()) {
            SysUserDO userDO = userMapper.selectById(i);
            if(userDO==null)throw new QueryException("所分配老师中有人未在数据库中");
            LogUtils.logContent(userDO.getName()+"老师去听的课：第"+courInfDO.getWeek()+"周，星期"
                    +courInfDO.getDay()+"，第"+courInfDO.getStartTime()+"-"+courInfDO.getEndTime()+"节，"+name+"课程。位置："+courInfDO.getLocation()+name+"课程");
        }
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM,String.valueOf(semId));
        return map;
    }

    private void judgeAlsoHasCourse(Integer semId,List<Integer> evaTeacherIdList, CourInfDO courInfDO) {
        List<Integer> list = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semId).in(!evaTeacherIdList.isEmpty(),"teacher_id", evaTeacherIdList)).stream().map(CourseDO::getId).toList();
        if(list.isEmpty()) return;
        for (Integer i : list) {

            CourInfDO courInfDO1 = courInfMapper.selectOne(new QueryWrapper<CourInfDO>().eq("week", courInfDO.getWeek())
                    .eq("day", courInfDO.getDay())
                    .le("start_time", courInfDO.getEndTime())
                    .ge("end_time", courInfDO.getStartTime())
                    .eq(true, "course_id", i));
            if(courInfDO1!=null){
                CourseDO courseDO = courseMapper.selectById(i);
                SysUserDO userDO = userMapper.selectById(courseDO.getTeacherId());
                throw new UpdateException(userDO.getName()+"老师"+"该时间段已有课程");

            }
        }

    }

    private void judgeAlsoHasTask(List<Integer> userList,CourInfDO courInfDO){
        List<Integer> courInfoList = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in(!userList.isEmpty(),"teacher_id", userList).eq("status",0)).stream().map(EvaTaskDO::getCourInfId).toList();
        if(courInfoList.isEmpty()){
            return;
        }
        for (Integer i : courInfoList) {
            CourInfDO courInfDO1 = courInfMapper.selectOne(new QueryWrapper<CourInfDO>()
                    .eq("week", courInfDO.getWeek())
                    .eq("day", courInfDO.getDay())
                    .le("start_time", courInfDO.getEndTime())
                    .ge("end_time", courInfDO.getStartTime())
                    .eq(true, "id", i));
            if(courInfDO1!=null){
                EvaTaskDO evaTaskDO = evaTaskMapper.selectOne(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courInfDO1.getId()).in("teacher_id", userList));
                SysUserDO userDO = userMapper.selectById( evaTaskDO.getTeacherId());
                throw new UpdateException("课程时间冲突，评教老师中"+userDO.getName()+"在该时间段已经有了评教任务");
            }

        }


    }

    @Override
    @Transactional
    public Map<String,Map<Integer,Integer>> importCourseFile(Map<String, List<CourseExcelBO>> courseExce, SemesterCO semester, Integer type) {
        SemesterDO semesterDO = semesterMapper.selectOne(new QueryWrapper<SemesterDO>().eq("start_year", semester.getStartYear()).eq("period", semester.getPeriod()));
        Map<String,Map<Integer,Integer>> map=new HashMap<>();
        String typeName=null;
        if(type==0)typeName="理论课";
        else typeName="实验课";
        Map<Integer,Integer> evaTaskIds=new HashMap<>();
        if(semesterDO!=null){
            Boolean imported = isImported(type, toTerm(semester));
            //执行已有学期的删除添加逻辑
            if(semester.getStartDate()!=null){
                semesterDO.setStartDate(semester.getStartDate());
                semesterMapper.update(semesterDO,new QueryWrapper<SemesterDO>().eq("id",semesterDO.getId()));
            }
            evaTaskIds=courseImportExce.deleteCourse(semesterDO.getId(),type);
            if(imported)map.put(semesterDO.getStartYear()+"-"+semesterDO.getEndYear()+"第"+(semesterDO.getPeriod()+1)+"学期"+typeName+"课程表被覆盖",null);
            else  map.put(semesterDO.getStartYear()+"-"+semesterDO.getEndYear()+"第"+(semesterDO.getPeriod()+1)+"学期"+typeName+"课程表被导入",null);
            map.put("因为"+semesterDO.getStartYear()+"-"+semesterDO.getEndYear()+"第"+(semesterDO.getPeriod()+1)+"学期"+typeName+"课程表被覆盖"+",故而取消您该学期的评教任务",evaTaskIds);
        }else{
            //直接插入学期
            SemesterDO semesterDO1 = courseConvertor.toSemesterDO(semester);
            semesterMapper.insert(semesterDO1);
            semesterDO=semesterDO1;
            map.put(semesterDO.getStartYear()+"-"+semesterDO.getEndYear()+"第"+(semesterDO.getPeriod()+1)+"学期"+typeName+"课程表被导入",null);
        }
        courseImportExce.addAll(courseExce, type,semesterDO.getId());
        localCacheManager.invalidateCache(null,classroomCacheConstants.ALL_CLASSROOM);

        LogUtils.logContent(semesterDO.getStartYear()+"-"+semesterDO.getEndYear()+"第"+semesterDO.getPeriod()+1+"学期"+typeName+"课程表");


        return map;
    }

    private Term toTerm(SemesterCO semester) {
        return new Term().setStartYear(semester.getStartYear()).setEndYear(semester.getEndYear()).setPeriod(semester.getPeriod());
    }

    @Override
    @Transactional
    public Map<String,Map<Integer,Integer>> updateSelfCourse(String userName, SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeInfoCO> timeList) {
        String msg=null;

        SysUserDO userDO = userMapper.selectOne(new QueryWrapper<SysUserDO>().eq("username", userName));
        if(userDO==null){
            throw new QueryException("用户不存在");
        }
        Integer userId =userDO.getId();
        CourseDO courseDO = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", selfTeachCourseCO.getId()).eq("teacher_id", userId));
        if(courseDO==null){
            //课程不存在(抛出异常)
            throw new QueryException("用户对应课程不存在");
        }
        List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", userId).eq("semester_id", courseDO.getSemesterId()));
        courseDOS.removeIf(aDo -> aDo.getId().equals(selfTeachCourseCO.getId()));
        SubjectDO subjectDO = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", courseDO.getSubjectId()));
        if(subjectDO==null)throw new QueryException("该课程对应的科目不存在");
        msg=toJudge(courseDO,subjectDO,selfTeachCourseCO);
        //课程类型
        msg+=JudgeCourseType(courseDO,selfTeachCourseCO);
        //课程时间段
        Map<Integer,Integer> taskMap=new HashMap<>();
        String msgEva="";
        msgEva=JudgeCourseTime(courseDO,timeList,courseDOS,selfTeachCourseCO,taskMap);
        if(!msgEva.isEmpty())msg+=selfTeachCourseCO.getName()+"课程的上课时间被修改了。";
        Map<String,Map<Integer,Integer>> map=new HashMap<>();
        map.put(msg,null);
        map.put(msgEva,taskMap);
        localCacheManager.invalidateCache(null,classroomCacheConstants.ALL_CLASSROOM);
        return map;
    }

    private String JudgeCourseTime(CourseDO courseDO, List<SelfTeachCourseTimeInfoCO> timeList,List<CourseDO> courseDOList,SelfTeachCourseCO selfTeachCourseCO,Map<Integer,Integer> taskMap) {
        String msg="";
        List<Integer> courInfoIds = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id", courseDO.getTeacherId()).eq("status",0)).stream().map(EvaTaskDO::getCourInfId).toList();
        List<CourInfDO> courInfoList = courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id", courseDO.getId()));
        List<CourInfDO> courseChangeList=new ArrayList<>();
        for (SelfTeachCourseTimeInfoCO selfTeachCourseTimeCO : timeList) {
            for (Integer week : selfTeachCourseTimeCO.getWeeks()) {
               CourInfDO courInfDO=new CourInfDO();
               courInfDO.setCourseId(courseDO.getId());
               courInfDO.setWeek(week);
               courInfDO.setDay(selfTeachCourseTimeCO.getDay());
               courInfDO.setStartTime(selfTeachCourseTimeCO.getStartTime());
               courInfDO.setEndTime(selfTeachCourseTimeCO.getEndTime());
//               courInfDO.setLocation(selfTeachCourseTimeCO.getClassroom());
                for (String s : selfTeachCourseTimeCO.getClassroom()) {
                    courInfDO.setLocation(s);
                    courseChangeList.add(ObjectUtil.clone(courInfDO));
                }
            }
        }
        List<CourInfDO> difference = getDifference(courseChangeList, courInfoList);
        List<CourInfDO> difference2 = getDifference( courInfoList,courseChangeList);
        if(difference.isEmpty()){
            for (CourInfDO courInfDO : difference2) {
                courInfMapper.delete(new QueryWrapper<CourInfDO>()
                        .eq("course_id", courInfDO.getCourseId())
                        .eq("week", courInfDO.getWeek()).eq("day", courInfDO.getDay())
                        .eq("start_time", courInfDO.getStartTime()).eq("end_time",courInfDO.getEndTime()));
                evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courInfDO.getId())).forEach(evaTaskDO -> taskMap.put(evaTaskDO.getId(),evaTaskDO.getTeacherId()));
                EvaTaskDO evaTaskDO=new EvaTaskDO();
                evaTaskDO.setStatus(2);
                evaTaskMapper.update(evaTaskDO,new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courInfDO.getId()));
            }
            localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(courseDO.getSemesterId()));
            if(taskMap.isEmpty()) return "";
            else return msg+selfTeachCourseCO.getName()+"课程的上课时间被修改了,"+"因而取消您对该课程的评教任务";
        }else{
            for (CourInfDO courInfDO : difference2) {
                courInfMapper.delete(new QueryWrapper<CourInfDO>()
                        .eq("course_id", courInfDO.getCourseId())
                        .eq("week", courInfDO.getWeek()).eq("day", courInfDO.getDay())
                        .eq("start_time", courInfDO.getStartTime()).eq("end_time",courInfDO.getEndTime()));
                evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courInfDO.getId())).forEach(evaTaskDO -> taskMap.put(evaTaskDO.getId(),evaTaskDO.getTeacherId()));
                EvaTaskDO evaTaskDO=new EvaTaskDO();
                evaTaskDO.setStatus(2);
                evaTaskMapper.update(evaTaskDO,new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courInfDO.getId()));
            }
            localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(courseDO.getSemesterId()));
            for (CourInfDO courInfDO : difference) {
                for(CourseDO course : courseDOList){
                    QueryWrapper<CourInfDO> wrapper = new QueryWrapper<CourInfDO>()
                            .eq("week", courInfDO.getWeek())
                            .eq("day", courInfDO.getDay())
                            .le("start_time", courInfDO.getEndTime())
                            .ge("end_time", courInfDO.getStartTime())
                            .eq("course_id", course.getId());
                    //判断对应时间段是否已经有课了
                    if(!courInfMapper.selectList(wrapper).isEmpty()){
                        throw new UpdateException("该时间段你已经有课了");
                    }
                }

                //评教
                if(!courInfoIds.isEmpty()){
                    QueryWrapper<CourInfDO> wrapper = new QueryWrapper<CourInfDO>()
                            .eq("week", courInfDO.getWeek())
                            .eq("day", courInfDO.getDay())
                            .le("start_time", courInfDO.getEndTime())
                            .ge("end_time", courInfDO.getStartTime())
                            .in(true,"course_id", courInfoIds);
                    if(!courInfMapper.selectList(wrapper).isEmpty()){
                        throw new UpdateException("该时间段你有要去评教的课程");
                    }
                }



                //判断对应时间段的教室是否被占用
                QueryWrapper<CourInfDO> wrapper = new QueryWrapper<CourInfDO>()
                        .eq("week", courInfDO.getWeek())
                        .eq("day", courInfDO.getDay())
                        .le("start_time", courInfDO.getEndTime())
                        .ge("end_time", courInfDO.getStartTime())
                        .and(courseWrapper->courseWrapper.ne("course_id",courseDO.getId()));
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
                                && Objects.equals(courseChange.getEndTime(), courInfo.getEndTime())
                                &&Objects.equals(courseChange.getLocation(), courInfo.getLocation())))
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
        List<Integer> typeIdIn = courseTypeMapper.selectList(new QueryWrapper<CourseTypeDO>().in(!typeName.isEmpty(),"name", typeName))
                .stream()
                .map(CourseTypeDO::getId)
                .sorted()
                .toList();

        // 比较两个集合
        if (typeIdIn.equals(typeIdDo)) {
            return msg;
        } else {
            courseTypeCourseMapper.delete(new QueryWrapper<CourseTypeCourseDO>().eq("course_id",courseDO.getId() ).in(!typeIdDo.isEmpty(),"type_id", typeIdDo));

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

    private String toJudge(CourseDO courseDO,SubjectDO subjectDO, SelfTeachCourseCO selfTeachCourseCO) {
        String msg="";
        String name = subjectDO.getName();
        //0: 理论课相关默认；1: 实验课相关默认；
        String natureExp = subjectDO.getNature().equals(0) ? "理论课" : "实践课";
        if(!subjectDO.getName().equals(selfTeachCourseCO.getName())||!subjectDO.getNature().equals(selfTeachCourseCO.getNature())){
            SubjectDO subject=new SubjectDO();
            subject.setNature(selfTeachCourseCO.getNature());
            subject.setName(selfTeachCourseCO.getName());
            subject.setCreateTime(LocalDateTime.now());
            subject.setUpdateTime(LocalDateTime.now());
            subjectMapper.insert(subject);
           //顺便将课程的subjectId更新
           CourseDO course=new CourseDO();
           course.setSubjectId(subject.getId());
           courseMapper.update(course, new QueryWrapper<CourseDO>().eq("id", selfTeachCourseCO.getId()));
            localCacheManager.invalidateCache(courseCacheConstants.SUBJECT_LIST,courseCacheConstants.COURSE_LIST_BY_SEM+courseDO.getSemesterId());
                return msg+name+"课程的名称被改成了"+subjectDO.getName()+"，类型是"+natureExp+"。";
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
        CourseDO courseDO = courseMapper.selectById(courseId);
        if(courseDO==null)throw new  QueryException("不存在对应的课程");
        SubjectDO subjectDO = subjectMapper.selectById(courseDO.getSubjectId());
        if(subjectDO==null)throw  new QueryException("不存在对应的课程的科目");
        LogUtils.logContent(subjectDO.getName()+"(ID:"+courseDO.getId()+")的课程的课数");
        localCacheManager.invalidateCache(null,classroomCacheConstants.ALL_CLASSROOM);
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
            localCacheManager.invalidateCache(null,courseCacheConstants.SUBJECT_LIST);
        }else {
            subjectId=subjectDO1.getId();
        }
        //向course表中插入数据
        CourseDO courseDO = courseConvertor.toCourseDO(courseInfo, subjectId, teacherId, semId);
        Integer type=null;
        if(courseDO.getTemplateId()==null&&(courseInfo.getSubjectMsg().getNature()==1|| courseInfo.getSubjectMsg().getNature()==0)){
            Integer id = formTemplateMapper.selectOne(new QueryWrapper<FormTemplateDO>().eq("is_default", courseInfo.getSubjectMsg().getNature())).getId();
            courseDO.setTemplateId(id);
             type = courseTypeMapper.selectOne(new QueryWrapper<CourseTypeDO>().eq("is_default", courseInfo.getSubjectMsg().getNature())).getId();
        }
        courseMapper.insert(courseDO);
        localCacheManager.invalidateCache(courseCacheConstants.COURSE_LIST_BY_SEM, String.valueOf(semId));
        //再根据teacherId和subjectId又将他查出来
       Integer courseDOId = courseDO.getId();
       if(type!=null&&!courseInfo.getTypeIdList().contains(type)){
           CourseTypeCourseDO courseTypeCourseDO = new CourseTypeCourseDO();
           courseTypeCourseDO.setCourseId(courseDOId);
           courseTypeCourseDO.setTypeId(type);
           courseTypeCourseDO.setCreateTime(courseInfo.getCreateTime());
           courseTypeCourseDO.setUpdateTime(courseInfo.getUpdateTime());
           courseTypeCourseMapper.insert(courseTypeCourseDO);
       }
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
        localCacheManager.invalidateCache(null,classroomCacheConstants.ALL_CLASSROOM);

    }

    @Override
    public Boolean isImported(Integer type, Term term) {
   /*     DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(term.getStartDate(), formatter);*/
        SemesterDO semesterDO = semesterMapper.selectOne(new QueryWrapper<SemesterDO>().eq("period", term.getPeriod()).eq("start_year", term.getStartYear()).eq("end_year", term.getEndYear()));
        if(semesterDO==null)return false;
        List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semesterDO.getId()));
        if(courseDOS.isEmpty())return false;
        List<Integer> list = courseDOS.stream().map(CourseDO::getSubjectId).toList();
        List<SubjectDO> subjectDOS = subjectMapper.selectList(new QueryWrapper<SubjectDO>().in(!list.isEmpty(),"id",list ));
        for (SubjectDO subjectDO : subjectDOS) {
            if(subjectDO.getNature().equals(type)){
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public void updateCoursesType(UpdateCoursesToTypeCmd updateCoursesToTypeCmd) {
        List<Integer> courseIdList = updateCoursesToTypeCmd.getCourseIdList();
        if(courseIdList==null||courseIdList.isEmpty())throw new UpdateException("请选择要更改类型的课程");
        List<Integer> typeIdList = updateCoursesToTypeCmd.getTypeIdList();
        if(typeIdList==null||typeIdList.isEmpty())throw new UpdateException("请选择要更改的类型");
        for (Integer i : courseIdList) {
            if(!courseMapper.exists(new QueryWrapper<CourseDO>().eq("id",i)))throw new QueryException("该课程已被删除");
            courseTypeCourseMapper.delete(new QueryWrapper<CourseTypeCourseDO>().eq("course_id",i));
            for (Integer integer : typeIdList) {
                if(!courseTypeMapper.exists(new QueryWrapper<CourseTypeDO>().eq("id",integer)))throw new QueryException("该课程类型那个已被删除");
                CourseTypeCourseDO courseTypeCourseDO = new CourseTypeCourseDO();
                courseTypeCourseDO.setCourseId(i);
                courseTypeCourseDO.setTypeId(integer);
                courseTypeCourseDO.setUpdateTime(LocalDateTime.now());
                courseTypeCourseDO.setCreateTime(LocalDateTime.now());
                courseTypeCourseMapper.insert(courseTypeCourseDO);
            }
        }
    }
}
