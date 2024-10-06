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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
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
        //更新课程信息类型（先删除，再添加）
        courseTypeCourseMapper.delete(new QueryWrapper<CourseTypeCourseDO>().eq("course_id",updateCourseCmd.getId()));
        //更新课程类型快照表
        for (Integer typeId : updateCourseCmd.getTypeIdList()) {
           CourseTypeCourseDO courseTypeCourseDO = new CourseTypeCourseDO();
           courseTypeCourseDO.setCourseId(updateCourseCmd.getId());
           courseTypeCourseDO.setTypeId(typeId);
           courseTypeCourseMapper.insert(courseTypeCourseDO);
        }
        //更新课程表的templateId字段
        CourseDO courseDO = new CourseDO();
        courseDO.setTemplateId(updateCourseCmd.getTemplateId());
        courseMapper.update(courseDO,new QueryWrapper<CourseDO>().eq("id",updateCourseCmd.getId()));
        if(updateCourseCmd.getIsUpdate()){
            //先查出课程表中的subjectId
            Integer subjectId = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", updateCourseCmd.getId())).getSubjectId();
            //再根据subjectId更新对应科目表
            subjectMapper.update(courseConvertor.toSubjectDO(updateCourseCmd.getSubjectMsg()),new QueryWrapper<SubjectDO>().eq("id",subjectId));
        }


        return null;
    }

    @Override
    @Transactional
    public Void updateSingleCourse(Integer semId, UpdateSingleCourseCmd updateSingleCourseCmd) {
        //更新一节课的数据
        CourInfDO courInfDO = courseConvertor.toCourInfDO(updateSingleCourseCmd);
        courInfDO.setUpdateTime(LocalDateTime.now());
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
        }
        //不存在可以抛出异常
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
            throw new RuntimeException("没有该用户对应课程");
        }
        //修改courseDo的updateTime
        courseDO.setUpdateTime(LocalDateTime.now());
        courseMapper.update(courseDO,new QueryWrapper<CourseDO>().eq("id", selfTeachCourseCO.getId()).eq("teacher_id", userId));
        //找出课程对应的科目实体类
        SubjectDO subjectDO = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", courseDO.getSubjectId()));
        //修改subject
        subjectDO.setName(selfTeachCourseCO.getName());
        subjectDO.setNature(selfTeachCourseCO.getNature());
        subjectDO.setUpdateTime(LocalDateTime.now());
        subjectMapper.update(subjectDO,new QueryWrapper<SubjectDO>().eq("id", courseDO.getSubjectId()));
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
                //先判断对应时间段的教室是否被占用
                if(courInfMapper.selectOne(new QueryWrapper<CourInfDO>().eq("classroom", selfTeachCourseTimeCO.getClassroom()).eq("course_id", selfTeachCourseCO.getId()).eq("week", week).eq("day", selfTeachCourseTimeCO.getDay()).eq("start_time", selfTeachCourseTimeCO.getStartTime()))!=null){
                    //被占用了，抛出异常
                    throw new RuntimeException("该时间段教室已占用");
                }
                CourInfDO courInfDO = courseConvertor.toCourInfDO(selfTeachCourseTimeCO, week, courseDO.getId(),LocalDateTime.now());
                courInfMapper.insert(courInfDO);

            }

        }

        return null;
    }
}
