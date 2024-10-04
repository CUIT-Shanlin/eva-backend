package edu.cuit.infra.gateway.impl.courseimpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.client.dto.cmd.course.AlignTeacherCmd;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import edu.cuit.client.dto.cmd.course.UpdateSingleCourseCmd;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import edu.cuit.infra.convertor.course.CourseConvertor;
import edu.cuit.infra.dal.database.dataobject.course.*;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.mapper.course.*;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
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
    public Void updateSingleCourse(Integer semId, UpdateSingleCourseCmd updateSingleCourseCmd) {
        //更新一节课的数据
        CourInfDO courInfDO = courseConvertor.toCourInfDO(updateSingleCourseCmd);
        courInfDO.setUpdateTime(LocalDateTime.now());
        courInfMapper.update(courInfDO,new QueryWrapper<CourInfDO>().eq("id",updateSingleCourseCmd.getId()));

        return null;
    }

    @Override
    public Void updateCourseType(CourseType courseType) {
        //根据id更新课程类型
        courseTypeMapper.update(courseConvertor.toCourseTypeDO(courseType),new QueryWrapper<CourseTypeDO>().eq("id",courseType.getId()));
        return null;
    }

    @Override
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
    public Void addCourse(Integer semId) {
        //TODO（接口已删除）
        return null;
    }

    @Override
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
    public Void importCourseFile(InputStream file) {
        //TODO
        return null;
    }
}
