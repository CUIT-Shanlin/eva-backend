package edu.cuit.infra.bcevaluation.query;

import edu.cuit.bc.evaluation.application.port.EvaRecordQueryPort;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaLogConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 评教记录读侧查询端口实现（委托 QueryRepo，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class EvaRecordQueryPortImpl implements EvaRecordQueryPort {
    private final EvaQueryRepo repo;

    @Override
    public PaginationResultEntity<EvaRecordEntity> pageEvaRecord(Integer semId, PagingQuery<EvaLogConditionalQuery> evaLogQuery) {
        return repo.pageEvaRecord(semId, evaLogQuery);
    }

    @Override
    public List<EvaRecordEntity> getEvaLogInfo(Integer userId, Integer id, String keyword) {
        return repo.getEvaLogInfo(userId, id, keyword);
    }

    @Override
    public List<EvaRecordEntity> getEvaEdLogInfo(Integer userId, Integer semId, Integer courseId) {
        return repo.getEvaEdLogInfo(userId, semId, courseId);
    }

    @Override
    public Optional<Double> getScoreFromRecord(String prop) {
        return repo.getScoreFromRecord(prop);
    }

    @Override
    public List<EvaRecordEntity> getRecordByCourse(Integer courseId) {
        return repo.getRecordByCourse(courseId);
    }

    @Override
    public Optional<Double> getScoreByProp(String prop) {
        return repo.getScoreByProp(prop);
    }

    @Override
    public List<Double> getScoresByProp(String props) {
        return repo.getScoresByProp(props);
    }

    @Override
    public Map<String, Double> getScorePropMapByProp(String props) {
        return repo.getScorePropMapByProp(props);
    }

    @Override
    public Optional<Integer> getEvaNumByCourInfo(Integer courInfId) {
        return repo.getEvaNumByCourInfo(courInfId);
    }

    @Override
    public Optional<Integer> getEvaNumByCourse(Integer courseId) {
        return repo.getEvaNumByCourse(courseId);
    }
}
