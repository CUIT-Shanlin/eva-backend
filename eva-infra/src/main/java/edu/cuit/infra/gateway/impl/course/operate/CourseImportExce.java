package edu.cuit.infra.gateway.impl.course.operate;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.client.bo.CourseExcelBO;
import edu.cuit.client.dto.clientobject.course.SubjectCO;
import edu.cuit.infra.convertor.course.CourseConvertor;
import edu.cuit.infra.dal.database.dataobject.course.*;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.*;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CourseImportExce {
    private final CourseConvertor courseConvertor;
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final CourseTypeCourseMapper courseTypeCourseMapper;
    private final CourseTypeMapper courseTypeMapper;
    private final SubjectMapper subjectMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final FormRecordMapper recordMapper;
    private final CourOneEvaTemplateMapper courOneEvaTemplateMapper;
    private final SysUserMapper userMapper;
    private final FormTemplateMapper formTemplateMapper;
    //删除这学期所有的课程
    public void deleteCourse(Integer semId) {
        //先找出所有的课程
        List<Integer> courseIdList = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semId)).stream().map(CourseDO::getId).toList();
        courseMapper.delete(new QueryWrapper<CourseDO>().eq("semester_id", semId));
       //删除每节课
        List<Integer> courInfoIds = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in(!courseIdList.isEmpty(),"course_id", courseIdList)).stream().map(CourInfDO::getId).toList();
        courInfMapper.delete(new QueryWrapper<CourInfDO>().in(!courseIdList.isEmpty(),"course_id", courseIdList));
        //删除评教任务
        List<EvaTaskDO> taskDOList = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in(!courInfoIds.isEmpty(),"cour_inf_id", courInfoIds));
        if(!taskDOList.isEmpty()) {
            List<Integer> taskIds = taskDOList.stream().map(EvaTaskDO::getId).toList();
            evaTaskMapper.deleteBatchIds(taskIds);
            //删除评教表单记录
            recordMapper.delete(new QueryWrapper<FormRecordDO>().in(!taskIds.isEmpty(),"task_id", taskIds));
            //删除评教快照
            courOneEvaTemplateMapper.delete(new QueryWrapper<CourOneEvaTemplateDO>().in(!courseIdList.isEmpty(),"course_id", courseIdList));
        }

    }
    public void addAll( Map<String, List<CourseExcelBO>> courseExce, Integer type,Integer semId){
        for (Map.Entry<String, List<CourseExcelBO>> stringListEntry : courseExce.entrySet()) {
            SubjectDO subjectDO1 = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("name", stringListEntry.getKey()));
            Integer id = null;
            if(subjectDO1==null){
                SubjectDO subjectDO = addSubject(stringListEntry.getKey(), type);
                subjectMapper.insert(subjectDO);
                id=subjectDO.getId();
            }else{
                id=subjectDO1.getId();
            }
            for (CourseExcelBO courseExcelBO : stringListEntry.getValue()) {
                if(courseExcelBO.getProfTitle()==null){
                    courseExcelBO.setProfTitle("讲师");
                }
                SysUserDO userDO = userMapper.selectOne(new QueryWrapper<SysUserDO>().eq("name", courseExcelBO.getTeacherName()).eq("prof_title", courseExcelBO.getProfTitle()));
                if(userDO==null){//如果老师不存在就把，课程就放弃
                    subjectMapper.deleteById(id);
                    continue;
                }
                Integer userId = userDO.getId();

               //评教表单模版id
                Integer evaTemplateId = getEvaTemplateId(type);
                CourseDO courseDO = addCourse(courseExcelBO, id, userId, semId,evaTemplateId);
                courseMapper.insert(courseDO);
                //课程类型课程关联表
                toInsert(courseDO.getId(), type);
                for (Integer week : courseExcelBO.getWeeks()) {
                    CourInfDO courInfDO = courseConvertor.toCourInfDO(courseDO.getId(), week, courseExcelBO, LocalDateTime.now());
                    courInfMapper.insert(courInfDO);
                }

            }

        }
    }
    private SubjectDO addSubject(String subjectName, Integer nature) {
        SubjectDO subjectDO=new SubjectDO();
        subjectDO.setName(subjectName);
        subjectDO.setNature(nature);
        subjectDO.setCreateTime(LocalDateTime.now());
        subjectDO.setUpdateTime(LocalDateTime.now());
        return subjectDO;
    }
    private CourseDO addCourse(CourseExcelBO courseExcelBO, Integer subjectId, Integer userId,Integer semId,Integer evaTemplateId) {
      CourseDO courseDO=new CourseDO();
      courseDO.setSubjectId(subjectId);
      courseDO.setTeacherId(userId);
      courseDO.setSemesterId(semId);
      courseDO.setTemplateId(evaTemplateId);
      courseDO.setCreateTime(LocalDateTime.now());
      courseDO.setUpdateTime(LocalDateTime.now());
      return courseDO;
    }
    private Integer getEvaTemplateId(Integer type){
    return formTemplateMapper.selectOne(new QueryWrapper<FormTemplateDO>().eq("is_default", type)).getId();
    }
    private void toInsert(Integer courseId,Integer type){
        //根据type来找到课程类型
        Integer id = courseTypeMapper.selectOne(new QueryWrapper<CourseTypeDO>().eq("is_default", type)).getId();
        //插入课程类型课程关联表
        CourseTypeCourseDO courseTypeCourseDO = new CourseTypeCourseDO();
        courseTypeCourseDO.setCourseId(courseId);
        courseTypeCourseDO.setCourseId(id);
        courseTypeCourseDO.setCreateTime(LocalDateTime.now());
        courseTypeCourseDO.setUpdateTime(LocalDateTime.now());
        courseTypeCourseMapper.insert(courseTypeCourseDO);
    }
}
