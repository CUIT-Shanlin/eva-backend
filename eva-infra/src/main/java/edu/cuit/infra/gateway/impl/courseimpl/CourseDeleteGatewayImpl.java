package edu.cuit.infra.gateway.impl.courseimpl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import edu.cuit.domain.gateway.course.CourseDeleteGateway;
import edu.cuit.infra.dal.database.dataobject.course.*;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.mapper.course.*;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
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

    @Override
    public Void deleteCourses(Integer semId, Integer id, Integer startWeek, Integer endWeek) {
        //先根据semId和id来找出课程数据
        QueryWrapper<CourInfDO> courseWrapper=new QueryWrapper<>();
        courseWrapper.eq("course_id",id);
        courseWrapper.between("week",startWeek,endWeek);
        courInfMapper.delete(courseWrapper);

        return null;
    }


    @Override
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
        evaTaskMapper.delete(evaTaskWrapper);
        Integer taskId = evaTaskMapper.selectOne(evaTaskWrapper).getId();
        //根据任务Id删除评教表单记录
        UpdateWrapper<FormRecordDO> formRecordWrapper=new UpdateWrapper<>();
        formRecordWrapper.eq("task_id",taskId);
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

        //得到要删除的课程id集合
        List<Integer> courseIds = courseTypeCourseDOS.stream().map(CourseTypeCourseDO::getCourseId).toList();
        //通过课程id来找到对应科目的id集合
        List<Integer> subjectIds = courseIds.stream().map(courseId -> courseMapper.selectById(courseId).getSubjectId()).toList();
        // 将对应课程类型的逻辑删除
        UpdateWrapper<CourseTypeDO> courseTypeWrapper=new UpdateWrapper<>();
        courseTypeWrapper.in("id",ids);
        courseTypeMapper.delete(courseTypeWrapper);
        // 将课程表对应的课程逻辑删除
        UpdateWrapper<CourseDO> courseWrapper=new UpdateWrapper<>();
        courseWrapper.in("id",courseIds);
        courseMapper.delete(courseWrapper);
        //将课程对应的课程详情表也逻辑删除
        UpdateWrapper<CourInfDO> courInfoWrapper=new UpdateWrapper<>();
        courseWrapper.in("course_id",courseIds);

        courInfMapper.delete(courInfoWrapper);
        //将课程对应的科目表也逻辑删除
        UpdateWrapper<SubjectDO> subjectWrapper=new UpdateWrapper<>();
        courseWrapper.in("id",subjectIds);
        subjectMapper.delete( subjectWrapper);
        return null;
    }
}
