package edu.cuit.infra.bccourse.adapter;

import edu.cuit.bc.course.application.port.SemesterStartDateQueryPort;
import edu.cuit.infra.dal.database.dataobject.course.SemesterDO;
import edu.cuit.infra.dal.database.mapper.course.SemesterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/**
 * bc-course：按学期ID查询学期开始日期的查询端口适配器。
 *
 * <p>约束：保持行为不变；内部仅委托 {@link SemesterMapper#selectById}。</p>
 */
@Component
@RequiredArgsConstructor
public class SemesterStartDateQueryPortImpl implements SemesterStartDateQueryPort {
    private final SemesterMapper semesterMapper;

    @Override
    public Optional<LocalDate> findStartDateBySemesterId(Integer semesterId) {
        SemesterDO semesterDO = semesterMapper.selectById(semesterId);
        return Optional.ofNullable(semesterDO).map(SemesterDO::getStartDate);
    }
}

