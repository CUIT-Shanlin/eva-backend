package edu.cuit.infra.gateway.impl.courseimpl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import edu.cuit.domain.gateway.course.CourseDeleteGateway;
import edu.cuit.infra.dal.database.dataobject.course.*;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.*;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CourseDeleteGatewayImpl implements CourseDeleteGateway {
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final CourseTypeCourseMapper courseTypeCourseMapper;
    private final CourseTypeMapper courseTypeMapper;
    private final SemesterMapper semesterMapper;
    private final SubjectMapper subjectMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final FormRecordMapper formRecordMapper;
    private final SysUserMapper userMapper;


    @Override
    @Transactional
    public Void deleteCourses(Integer semId, Integer id, Integer startWeek, Integer endWeek) {
        //先根据semId和id来找出课程数据
        QueryWrapper<CourInfDO> courseWrapper=new QueryWrapper<>();
        courseWrapper.eq("course_id",id);
        courseWrapper.between("week",startWeek,endWeek);
        courInfMapper.delete(courseWrapper);

        return null;
    }


    @Override
    @Transactional
    public Void deleteCourse(Integer semId, Integer id) {
        //删除课程表
        UpdateWrapper<CourseDO> courseWrapper=new UpdateWrapper<>();
        courseWrapper.eq("id",id);
        courseWrapper.eq("semester_id",semId);
        courseMapper.delete(courseWrapper);
        //删除课程详情表
        UpdateWrapper<CourInfDO> courInfoWrapper=new UpdateWrapper<>();
        courseWrapper.eq("course_id",id);
        courInfMapper.delete(courInfoWrapper);
        //得到科目id
        Integer subjectId = courseMapper.selectOne(courseWrapper).getSubjectId();
        //删除科目数据
        UpdateWrapper<SubjectDO> subjectWrapper=new UpdateWrapper<>();
        subjectWrapper.eq("id",subjectId);
        subjectMapper.delete(subjectWrapper);
        //删除评教任务数据
        QueryWrapper<EvaTaskDO> evaTaskWrapper=new QueryWrapper<>();
        evaTaskWrapper.eq("cour_inf_id",id);
        List<Integer> taskIds = evaTaskMapper.selectList(evaTaskWrapper).stream().map(EvaTaskDO::getId).toList();
        evaTaskMapper.delete(evaTaskWrapper);
        //根据任务Id删除评教表单记录
        UpdateWrapper<FormRecordDO> formRecordWrapper=new UpdateWrapper<>();
        formRecordWrapper.in("task_id",taskIds);
        formRecordMapper.delete(formRecordWrapper);


        return null;
    }


    @Override
    @Transactional
    public Void deleteCourseType(List<Integer> ids) {
        QueryWrapper<CourseTypeCourseDO> wrapper = new QueryWrapper<>();
        wrapper.in("type_id",ids);
        //将courseTypeCourse逻辑删除
        courseTypeCourseMapper.delete(wrapper);
        List<CourseTypeCourseDO> courseTypeCourseDOS = courseTypeCourseMapper.selectList(wrapper);

        // 将对应课程类型的逻辑删除
        UpdateWrapper<CourseTypeDO> courseTypeWrapper=new UpdateWrapper<>();
        courseTypeWrapper.in("id",ids);
        courseTypeMapper.delete(courseTypeWrapper);

        return null;
    }

    @Override
    @Transactional
    public Void deleteSelfCourse(String userName, Integer courseId) {
        //先根据userName来找到用户id
        Integer userId = userMapper.selectOne(new QueryWrapper<SysUserDO>().eq("username", userName)).getId();
        //根据userId和courseId来删除课程表
        CourseDO courseDO = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", courseId).eq("teacher_id", userId));
        if(courseDO==null){
            //课程不存在(抛出异常)
            throw new RuntimeException("没有该用户对应课程");
        }
        courseMapper.delete(new UpdateWrapper<CourseDO>().eq("id", courseId).eq("teacher_id", userId));
        subjectMapper.delete(new UpdateWrapper<SubjectDO>().eq("id", courseDO.getSubjectId()));
        courInfMapper.delete(new UpdateWrapper<CourInfDO>().eq("course_id", courseId));
        //删除评教相关数据
        List<EvaTaskDO> taskIds = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("cour_inf_id", courseId));
        evaTaskMapper.delete(new UpdateWrapper<EvaTaskDO>().eq("cour_inf_id", courseId));
        formRecordMapper.delete(new UpdateWrapper<FormRecordDO>().in("task_id", taskIds));


        return null;
    }
}
