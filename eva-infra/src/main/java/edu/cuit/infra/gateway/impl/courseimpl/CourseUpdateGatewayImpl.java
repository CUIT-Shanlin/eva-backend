package edu.cuit.infra.gateway.impl.courseimpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import edu.cuit.client.dto.cmd.course.AlignTeacherCmd;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import edu.cuit.client.dto.cmd.course.UpdateSingleCourseCmd;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import edu.cuit.infra.convertor.course.CourseConvertor;
import edu.cuit.infra.dal.database.dataobject.course.*;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.*;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CourseUpdateGatewayImpl implements CourseUpdateGateway {
    private final CourseConvertor courseConvertor;
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final CourseTypeCourseMapper courseTypeCourseMapper;
    private final CourseTypeMapper courseTypeMapper;
    private final SemesterMapper semesterMapper;
    private final SubjectMapper subjectMapper;
    private final CourOneEvaTemplateMapper courOneEvaMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final SysUserMapper userMapper;

    @Override
    @Transactional
    public Void updateCourse(Integer semId, UpdateCourseCmd updateCourseCmd) {
        List<Integer> courseIdList=null;
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
        return null;
    }

    @Override
    @Transactional
    public Void updateSingleCourse(String userName,Integer semId, UpdateSingleCourseCmd updateSingleCourseCmd) {
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
                    .eq("start_time", updateSingleCourseCmd.getTime().getStartTime()));
            if (courInfDO != null) {
                throw new UpdateException("该时间段已有课程");
            }
        }
        //判断location是否被占用
            //先根据updateSingleCourseCmd中的数据找出所有对应时间段的课程
                List<CourInfDO> courInfDOList = courInfMapper.selectList(new QueryWrapper<CourInfDO>()
                        .eq("week", updateSingleCourseCmd.getTime().getWeek())
                        .eq("day", updateSingleCourseCmd.getTime().getDay())
                        .eq("start_time", updateSingleCourseCmd.getTime().getStartTime()));
                for (CourInfDO courInfDO : courInfDOList) {
                    if(courseMapper.selectById(courInfDO.getCourseId()).getSemesterId()==semId){
                        if(courInfDO.getLocation().equals(updateSingleCourseCmd.getLocation())){
                            throw new UpdateException("该时间段该地点已有课程");
                        }
                    }

                }


        //更新一节课的数据
        CourInfDO courInfDO = courseConvertor.toCourInfDO(updateSingleCourseCmd);
        courInfDO.setUpdateTime(LocalDateTime.now());
        courInfDO.setLocation(updateSingleCourseCmd.getLocation());
        courInfMapper.update(courInfDO,new QueryWrapper<CourInfDO>().eq("id",updateSingleCourseCmd.getId()));

        return null;
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
    public Void assignTeacher(Integer semId, AlignTeacherCmd alignTeacherCmd) {

        Integer courseId=alignTeacherCmd.getId();
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


        return null;
    }

    @Override
    @Transactional
    public Void importCourseFile(InputStream file) {
        //TODO
        return null;
    }

    @Override
    @Transactional
    public Void updateSelfCourse(String userName, SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeCO> timeList) {
        //先根据userName来找到用户id
        Integer userId = userMapper.selectOne(new QueryWrapper<SysUserDO>().eq("username", userName)).getId();
        CourseDO courseDO = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", selfTeachCourseCO.getId()).eq("teacher_id", userId));
        if(courseDO==null){
            //课程不存在(抛出异常)
            throw new QueryException("没有该用户对应课程");
        }
        //找出这个老师的其他课程
        List<CourseDO> courseDOList = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", userId).eq("semester_id", courseDO.getSemesterId()));
        //去掉courseDOList中的courseDO
        courseDOList.remove(courseDO);
        //修改courseDo的updateTime
        courseDO.setUpdateTime(LocalDateTime.now());
        courseMapper.update(courseDO,new QueryWrapper<CourseDO>().eq("id", selfTeachCourseCO.getId()).eq("teacher_id", userId));
/*        //找出课程对应的科目实体类
        SubjectDO subjectDO = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", courseDO.getSubjectId()));
        //修改subject
        subjectDO.setName(selfTeachCourseCO.getName());
        subjectDO.setNature(selfTeachCourseCO.getNature());
        subjectDO.setUpdateTime(LocalDateTime.now());
        subjectMapper.update(subjectDO,new QueryWrapper<SubjectDO>().eq("id", courseDO.getSubjectId()));*/
        //对于课程类型先删除再添加
        courseTypeCourseMapper.delete(new QueryWrapper<CourseTypeCourseDO>().eq("course_id", selfTeachCourseCO.getId()));
        for (CourseType type : selfTeachCourseCO.getTypeList()) {
            CourseTypeCourseDO courseTypeCourseDO = new CourseTypeCourseDO();
            courseTypeCourseDO.setCourseId(selfTeachCourseCO.getId());
            courseTypeCourseDO.setTypeId(type.getId());
            courseTypeCourseDO.setUpdateTime(LocalDateTime.now());
            courseTypeCourseDO.setCreateTime(LocalDateTime.now());
            courseTypeCourseMapper.insert(courseTypeCourseDO);
        }
        //修改课程时间表（先删除再添加）
        courInfMapper.delete(new QueryWrapper<CourInfDO>().eq("course_id", selfTeachCourseCO.getId()));
        //遍历timeList集合，来添加课程时间表
        for (SelfTeachCourseTimeCO selfTeachCourseTimeCO : timeList) {
            for (Integer week : selfTeachCourseTimeCO.getWeeks()) {
                QueryWrapper<CourInfDO> wrapper = new QueryWrapper<CourInfDO>()
                        .eq("week", week)
                        .eq("day", selfTeachCourseTimeCO.getDay())
                        .eq("start_time", selfTeachCourseTimeCO.getStartTime());
                for(CourseDO course : courseDOList){
                    wrapper.eq("course_id", selfTeachCourseCO.getId());
                    //判断对应时间段是否已经有课了
                    if(courInfMapper.selectOne(wrapper)!=null){
                        throw new UpdateException("该时间段你已经有课了");
                    }
                    //判断对应时间段的教室是否被占用
                    wrapper.eq("location", selfTeachCourseTimeCO.getClassroom());
                    if(courInfMapper.selectOne(wrapper)!=null){
                        //被占用了，抛出异常
                        throw new UpdateException("该时间段教室已占用");
                    }
                }
                CourInfDO courInfDO = courseConvertor.toCourInfDO(selfTeachCourseTimeCO, week, courseDO.getId(),LocalDateTime.now());
                courInfMapper.insert(courInfDO);
            }
            //根据课程Id来删除评教任务
            evaTaskMapper.delete(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", selfTeachCourseCO.getId()));


        }

        return null;
    }

    @Override
    @Transactional
    public Void addExistCoursesDetails(Integer courseId, SelfTeachCourseTimeCO timeCO) {
        for (Integer week : timeCO.getWeeks()) {
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

    @Override
    @Transactional
    public void addNotExistCoursesDetails(Integer semId, Integer teacherId, UpdateCourseCmd courseInfo, List<SelfTeachCourseTimeCO> dateArr) {
        SubjectDO subjectDO = courseConvertor.toSubjectDO(courseInfo.getSubjectMsg());
        //向subject表插入数据并返回主键ID
        subjectMapper.insert(subjectDO);
        Integer subjectId = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("name", subjectDO.getName())).getId();
        //向course表中插入数据
        CourseDO courseDO = courseConvertor.toCourseDO(courseInfo, subjectId, teacherId, semId);
        courseMapper.insert(courseDO);
        //再根据teacherId和subjectId又将他查出来
       Integer courseDOId = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("subject_id", subjectId).eq("teacher_id", teacherId)).getId();
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
}
