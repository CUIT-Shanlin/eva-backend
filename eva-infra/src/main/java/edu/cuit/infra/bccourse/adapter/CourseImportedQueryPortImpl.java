package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.CourseImportedQueryPort;
import edu.cuit.client.dto.data.Term;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SemesterDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SemesterMapper;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * bc-course：课表导入状态查询端口适配器（保持历史行为不变：原样搬运旧 gateway 查询逻辑）。
 */
@Component
@RequiredArgsConstructor
public class CourseImportedQueryPortImpl implements CourseImportedQueryPort {
    private final SemesterMapper semesterMapper;
    private final CourseMapper courseMapper;
    private final SubjectMapper subjectMapper;

    @Override
    public Boolean isImported(Integer type, Term term) {
        SemesterDO semesterDO = semesterMapper.selectOne(
                new QueryWrapper<SemesterDO>()
                        .eq("period", term.getPeriod())
                        .eq("start_year", term.getStartYear())
                        .eq("end_year", term.getEndYear())
        );
        if (semesterDO == null) return false;
        List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semesterDO.getId()));
        if (courseDOS.isEmpty()) return false;
        List<Integer> list = courseDOS.stream().map(CourseDO::getSubjectId).toList();
        List<SubjectDO> subjectDOS = subjectMapper.selectList(new QueryWrapper<SubjectDO>().in(!list.isEmpty(), "id", list));
        for (SubjectDO subjectDO : subjectDOS) {
            if (subjectDO.getNature().equals(type)) {
                return true;
            }
        }
        return false;
    }
}

