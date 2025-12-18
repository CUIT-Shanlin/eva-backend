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
import edu.cuit.infra.gateway.impl.course.operate.CourseFormat;
import edu.cuit.bc.course.application.model.ChangeCourseTemplateCommand;
import edu.cuit.bc.course.application.usecase.ChangeCourseTemplateUseCase;
import edu.cuit.bc.course.domain.ChangeCourseTemplateException;
import edu.cuit.bc.course.application.model.AssignEvaTeachersCommand;
import edu.cuit.bc.course.application.usecase.AssignEvaTeachersUseCase;
import edu.cuit.bc.course.domain.AssignEvaTeachersException;
import edu.cuit.bc.course.application.model.ImportCourseFileCommand;
import edu.cuit.bc.course.application.usecase.ImportCourseFileUseCase;
import edu.cuit.bc.course.domain.ImportCourseFileException;
import edu.cuit.bc.course.application.model.UpdateCourseInfoCommand;
import edu.cuit.bc.course.application.usecase.UpdateCourseInfoUseCase;
import edu.cuit.bc.course.domain.UpdateCourseInfoException;
import edu.cuit.bc.course.application.model.UpdateCourseTypeCommand;
import edu.cuit.bc.course.application.model.UpdateCoursesTypeCommand;
import edu.cuit.bc.course.application.usecase.UpdateCourseTypeUseCase;
import edu.cuit.bc.course.application.usecase.UpdateCoursesTypeUseCase;
import edu.cuit.bc.course.application.model.UpdateSingleCourseCommand;
import edu.cuit.bc.course.application.usecase.UpdateSingleCourseUseCase;
import edu.cuit.bc.course.domain.UpdateSingleCourseException;
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
    private final LocalCacheManager localCacheManager;
    private final CourseCacheConstants courseCacheConstants;
    private final FormTemplateMapper formTemplateMapper;
    private final EvaCacheConstants evaCacheConstants;
    private final ClassroomCacheConstants classroomCacheConstants;
    private final ChangeCourseTemplateUseCase changeCourseTemplateUseCase;
    private final AssignEvaTeachersUseCase assignEvaTeachersUseCase;
    private final UpdateSingleCourseUseCase updateSingleCourseUseCase;
    private final ImportCourseFileUseCase importCourseFileUseCase;
    private final UpdateCourseInfoUseCase updateCourseInfoUseCase;
    private final UpdateCourseTypeUseCase updateCourseTypeUseCase;
    private final UpdateCoursesTypeUseCase updateCoursesTypeUseCase;


    /**
     * 修改一门课程
     *@param semId 学期id
     *@param updateCourseCmd 修改课程信息
     *
     * */
    @Override
    @Transactional
    public Map<String,Map<Integer,Integer>> updateCourse(Integer semId, UpdateCourseCmd updateCourseCmd) {
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“修改课程信息”业务流程
        try {
            return updateCourseInfoUseCase.execute(new UpdateCourseInfoCommand(semId, updateCourseCmd));
        } catch (UpdateCourseInfoException e) {
            throw new UpdateException(e.getMessage());
        }
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
        // 历史路径：收敛到 bc-course 用例，避免基础设施层重复实现与重复校验
        try {
            changeCourseTemplateUseCase.execute(new ChangeCourseTemplateCommand(
                    semId,
                    updateCoursesCmd.getTemplateId(),
                    updateCoursesCmd.getCourseIdList()
            ));
        } catch (ChangeCourseTemplateException e) {
            throw new UpdateException(e.getMessage());
        }

    }

    @Override
    @Transactional
    public Map<String,Map<Integer,Integer>> updateSingleCourse(String userName,Integer semId, UpdateSingleCourseCmd updateSingleCourseCmd) {
        // 历史路径：收敛到 bc-course 用例（保持行为不变）
        try {
            return updateSingleCourseUseCase.execute(new UpdateSingleCourseCommand(
                    semId,
                    updateSingleCourseCmd.getId(),
                    updateSingleCourseCmd.getTime() == null ? null : updateSingleCourseCmd.getTime().getWeek(),
                    updateSingleCourseCmd.getTime() == null ? null : updateSingleCourseCmd.getTime().getDay(),
                    updateSingleCourseCmd.getTime() == null ? null : updateSingleCourseCmd.getTime().getStartTime(),
                    updateSingleCourseCmd.getTime() == null ? null : updateSingleCourseCmd.getTime().getEndTime(),
                    updateSingleCourseCmd.getLocation()
            ));
        } catch (UpdateSingleCourseException e) {
            throw new UpdateException(e.getMessage());
        }
    }

    @Override
    @Transactional
    public Void updateCourseType(UpdateCourseTypeCmd courseType) {
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“课程类型修改”业务流程
        updateCourseTypeUseCase.execute(new UpdateCourseTypeCommand(courseType));
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
        // 历史路径：收敛到 bc-course 用例，避免基础设施层继续堆业务规则
        try {
            return assignEvaTeachersUseCase.execute(new AssignEvaTeachersCommand(
                    semId,
                    alignTeacherCmd.getId(),
                    alignTeacherCmd.getEvaTeacherIdList()
            ));
        } catch (AssignEvaTeachersException e) {
            throw new UpdateException(e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String,Map<Integer,Integer>> importCourseFile(Map<String, List<CourseExcelBO>> courseExce, SemesterCO semester, Integer type) {
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“导入课表”业务流程
        try {
            return importCourseFileUseCase.execute(new ImportCourseFileCommand(courseExce, semester, type));
        } catch (ImportCourseFileException e) {
            throw new UpdateException(e.getMessage());
        }
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
        msg+=JudgeCourseType(userDO.getName()+"老师的"+selfTeachCourseCO.getName(),courseDO,selfTeachCourseCO);
        //课程时间段
        Map<Integer,Integer> taskMap=new HashMap<>();
        String msgEva="";
        msgEva=JudgeCourseTime(courseDO,timeList,courseDOS,selfTeachCourseCO,taskMap);
        if(!msgEva.isEmpty())msg+=userDO.getName()+"老师的"+selfTeachCourseCO.getName()+"课程的上课时间（教室）被修改了。";
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
        SysUserDO userDO = userMapper.selectById(courseDO.getTeacherId());
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
                        .eq("start_time", courInfDO.getStartTime()).eq("end_time",courInfDO.getEndTime())
                        .eq("location", courInfDO.getLocation()));
                evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courInfDO.getId())).forEach(evaTaskDO -> taskMap.put(evaTaskDO.getId(),evaTaskDO.getTeacherId()));
               /* EvaTaskDO evaTaskDO=new EvaTaskDO();
                evaTaskDO.setStatus(2);*/
                evaTaskMapper.delete(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courInfDO.getId()));
            }
            localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(courseDO.getSemesterId()));
            if(taskMap.isEmpty()) return "";
            else return msg+userDO.getName()+"老师的"+selfTeachCourseCO.getName()+"课程的上课时间（教室）被修改了,"+"因而取消您对该课程的评教任务";
        }else{
            for (CourInfDO courInfDO : difference2) {
                courInfMapper.delete(new QueryWrapper<CourInfDO>()
                        .eq("course_id", courInfDO.getCourseId())
                        .eq("week", courInfDO.getWeek()).eq("day", courInfDO.getDay())
                        .eq("start_time", courInfDO.getStartTime()).eq("end_time",courInfDO.getEndTime())
                        .eq("location", courInfDO.getLocation()));
                evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courInfDO.getId())).forEach(evaTaskDO -> taskMap.put(evaTaskDO.getId(),evaTaskDO.getTeacherId()));
           /*     EvaTaskDO evaTaskDO=new EvaTaskDO();
                evaTaskDO.setStatus(2);*/
                evaTaskMapper.delete(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courInfDO.getId()));
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
            return msg+userDO.getName()+"老师的"+selfTeachCourseCO.getName()+"课程的上课时间（教室）被修改了,"+"因而取消您对该课程的评教任务";
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

    private String JudgeCourseType(String info,CourseDO courseDO,SelfTeachCourseCO selfTeachCourseCO) {
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

            return msg+info+"课程类型被修改为:"+String.join(",", typeName)+"。";
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
            SysUserDO userDO = userMapper.selectById(courseDO.getTeacherId());
            localCacheManager.invalidateCache(courseCacheConstants.SUBJECT_LIST,courseCacheConstants.COURSE_LIST_BY_SEM+courseDO.getSemesterId());
                return msg+userDO.getName()+"老师的"+name+"课程的名称被改成了"+subjectDO.getName()+"，类型是"+natureExp+"。";
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
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“批量课程类型修改”业务流程
        updateCoursesTypeUseCase.execute(new UpdateCoursesTypeCommand(updateCoursesToTypeCmd));
    }
}
