package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.model.ImportCourseFileCommand;
import edu.cuit.bc.course.application.port.ImportCourseFileRepository;
import edu.cuit.client.bo.CourseExcelBO;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.client.dto.data.Term;
import edu.cuit.infra.convertor.course.CourseConvertor;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SemesterDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SemesterMapper;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import edu.cuit.infra.enums.cache.ClassroomCacheConstants;
import edu.cuit.infra.gateway.impl.course.operate.CourseImportExce;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * bc-course：课表导入端口适配器（复用现有表结构与规则，行为保持不变）。
 */
@Component
@RequiredArgsConstructor
public class ImportCourseFileRepositoryImpl implements ImportCourseFileRepository {
    private final CourseConvertor courseConvertor;
    private final SemesterMapper semesterMapper;
    private final CourseMapper courseMapper;
    private final SubjectMapper subjectMapper;
    private final CourseImportExce courseImportExce;
    private final LocalCacheManager localCacheManager;
    private final ClassroomCacheConstants classroomCacheConstants;

    @Override
    @Transactional
    public Map<String, Map<Integer, Integer>> importCourseFile(ImportCourseFileCommand command) {
        Map<String, List<CourseExcelBO>> courseExce = command.courseExce();
        SemesterCO semester = command.semester();
        Integer type = command.type();

        SemesterDO semesterDO = semesterMapper.selectOne(new QueryWrapper<SemesterDO>()
                .eq("start_year", semester.getStartYear())
                .eq("period", semester.getPeriod()));

        Map<String, Map<Integer, Integer>> map = new HashMap<>();
        String typeName;
        if (type == 0) {
            typeName = "理论课";
        } else {
            typeName = "实验课";
        }

        Map<Integer, Integer> evaTaskIds = new HashMap<>();
        if (semesterDO != null) {
            Boolean imported = isImported(type, toTerm(semester));
            // 执行已有学期的删除添加逻辑
            if (semester.getStartDate() != null) {
                semesterDO.setStartDate(semester.getStartDate());
                semesterMapper.update(semesterDO, new QueryWrapper<SemesterDO>().eq("id", semesterDO.getId()));
            }
            evaTaskIds = courseImportExce.deleteCourse(semesterDO.getId(), type);
            if (imported) {
                map.put(semesterDO.getStartYear() + "-" + semesterDO.getEndYear() + "第" + (semesterDO.getPeriod() + 1) + "学期" + typeName + "课程表被覆盖", null);
            } else {
                map.put(semesterDO.getStartYear() + "-" + semesterDO.getEndYear() + "第" + (semesterDO.getPeriod() + 1) + "学期" + typeName + "课程表被导入", null);
            }
            map.put("因为" + semesterDO.getStartYear() + "-" + semesterDO.getEndYear() + "第" + (semesterDO.getPeriod() + 1) + "学期" + typeName + "课程表被覆盖" + ",故而取消您该学期的评教任务", evaTaskIds);
        } else {
            // 直接插入学期
            SemesterDO semesterDO1 = courseConvertor.toSemesterDO(semester);
            semesterMapper.insert(semesterDO1);
            semesterDO = semesterDO1;
            map.put(semesterDO.getStartYear() + "-" + semesterDO.getEndYear() + "第" + (semesterDO.getPeriod() + 1) + "学期" + typeName + "课程表被导入", null);
        }
        courseImportExce.addAll(courseExce, type, semesterDO.getId());
        localCacheManager.invalidateCache(null, classroomCacheConstants.ALL_CLASSROOM);

        // 保持旧日志内容拼接方式不变（历史行为）
        LogUtils.logContent(semesterDO.getStartYear() + "-" + semesterDO.getEndYear() + "第" + semesterDO.getPeriod() + 1 + "学期" + typeName + "课程表");

        return map;
    }

    private Term toTerm(SemesterCO semester) {
        return new Term()
                .setStartYear(semester.getStartYear())
                .setEndYear(semester.getEndYear())
                .setPeriod(semester.getPeriod());
    }

    private Boolean isImported(Integer type, Term term) {
        SemesterDO semesterDO = semesterMapper.selectOne(new QueryWrapper<SemesterDO>()
                .eq("period", term.getPeriod())
                .eq("start_year", term.getStartYear())
                .eq("end_year", term.getEndYear()));
        if (semesterDO == null) {
            return false;
        }
        List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semesterDO.getId()));
        if (courseDOS.isEmpty()) {
            return false;
        }
        List<Integer> subjectIdList = courseDOS.stream().map(CourseDO::getSubjectId).toList();
        List<SubjectDO> subjectDOS = subjectMapper.selectList(new QueryWrapper<SubjectDO>().in(!subjectIdList.isEmpty(), "id", subjectIdList));
        for (SubjectDO subjectDO : subjectDOS) {
            if (subjectDO.getNature().equals(type)) {
                return true;
            }
        }
        return false;
    }
}

