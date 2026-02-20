package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import edu.cuit.bc.course.application.port.SubjectObjectDirectQueryPort;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link SubjectObjectDirectQueryPort} 的端口适配器实现（保持行为不变）。
 *
 * <p>说明：该适配器仅做“原样委托 + 返回”，不引入新的缓存/日志副作用。</p>
 */
@Component
@RequiredArgsConstructor
public class SubjectObjectDirectQueryPortImpl implements SubjectObjectDirectQueryPort {

    private final SubjectMapper subjectMapper;

    @Override
    public List<SubjectDO> findSubjectList(Wrapper<SubjectDO> queryWrapper) {
        return subjectMapper.selectList(queryWrapper);
    }

    @Override
    public SubjectDO findSubjectById(Integer subjectId) {
        return subjectMapper.selectById(subjectId);
    }
}

