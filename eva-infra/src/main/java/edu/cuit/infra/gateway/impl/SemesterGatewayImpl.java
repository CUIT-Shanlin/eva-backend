package edu.cuit.infra.gateway.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.domain.gateway.SemesterGateway;
import edu.cuit.infra.convertor.SemesterConverter;
import edu.cuit.infra.dal.database.dataobject.course.SemesterDO;
import edu.cuit.infra.dal.database.mapper.course.SemesterMapper;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SemesterGatewayImpl implements SemesterGateway {
    private final SemesterMapper semesterMapper;
    private final SemesterConverter semesterConverter;
    @Override
    public List<SemesterCO> getAll() {
        List<SemesterDO> semesterDOS = semesterMapper.selectList(null);
        return semesterDOS.stream().map(semesterConverter::toSemesterCO).toList();
    }

    @Override
    public SemesterCO getNow() {
        LocalDate now = LocalDate.now();
        //查询这一年的学期
        List<SemesterDO> startDate = semesterMapper.selectList(new QueryWrapper<SemesterDO>().le("start_date", now));
        // 遍历startDate集合，找到startDate最大的值，就是当前学期
        Optional<SemesterDO> optionalSemesterDO = startDate.stream()
                .max((o1, o2) -> o1.getStartDate().compareTo(o2.getStartDate()));
        // 检查Optional是否为空
        SemesterDO semesterDO = optionalSemesterDO.orElseThrow(() -> new QueryException("数据库中还没有学期数据"));

        return semesterConverter.toSemesterCO(semesterDO);
    }

    @Override
    public SemesterCO getSemesterInfo(Integer id) {
        if(id==null||id<0){
           return getNow();
        }
        return semesterConverter.toSemesterCO(semesterMapper.selectById(id));
    }
}
