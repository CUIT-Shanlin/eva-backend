package edu.cuit.infra.gateway.impl.eva;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.evaluation.application.port.DeleteEvaRecordRepository;
import edu.cuit.bc.evaluation.application.port.DeleteEvaTemplateRepository;
import edu.cuit.bc.evaluation.domain.DeleteEvaRecordQueryException;
import edu.cuit.bc.evaluation.domain.DeleteEvaRecordUpdateException;
import edu.cuit.bc.evaluation.domain.DeleteEvaTemplateQueryException;
import edu.cuit.bc.evaluation.domain.DeleteEvaTemplateUpdateException;
import edu.cuit.domain.gateway.eva.EvaDeleteGateway;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EvaDeleteGatewayImpl implements EvaDeleteGateway {
    private final EvaTaskMapper evaTaskMapper;
    private final DeleteEvaRecordRepository deleteEvaRecordRepository;
    private final DeleteEvaTemplateRepository deleteEvaTemplateRepository;
    @Override
    @Transactional
    public Void deleteEvaRecord(List<Integer> ids) {
        try {
            deleteEvaRecordRepository.delete(ids);
        } catch (DeleteEvaRecordQueryException e) {
            throw new QueryException(e.getMessage());
        } catch (DeleteEvaRecordUpdateException e) {
            throw new UpdateException(e.getMessage());
        }
        return null;
    }
    //ok
    @Override
    @Transactional
    public Void deleteEvaTemplate(List<Integer> ids) {
        try {
            deleteEvaTemplateRepository.delete(ids);
        } catch (DeleteEvaTemplateQueryException e) {
            throw new QueryException(e.getMessage());
        } catch (DeleteEvaTemplateUpdateException e) {
            throw new UpdateException(e.getMessage());
        }
        return null;
    }

    @Override
    public List<Integer> deleteAllTaskByTea(Integer teacherId) {
        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id",teacherId));
        List<Integer> evaTaskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
        if(CollectionUtil.isEmpty(evaTaskDOS)){
            return List.of();
        }
        evaTaskMapper.delete(new QueryWrapper<EvaTaskDO>().eq("teacher_id",teacherId));
        return evaTaskIds;
    }
}
