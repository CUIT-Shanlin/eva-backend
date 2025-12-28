package edu.cuit.app.eva;

import com.alibaba.cola.exception.SysException;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.app.convertor.eva.EvaRecordBizConvertor;
import edu.cuit.app.service.impl.eva.EvaRecordServiceImpl;
import edu.cuit.bc.evaluation.application.port.EvaRecordPagingQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaRecordScoreQueryPort;
import edu.cuit.bc.evaluation.application.usecase.DeleteEvaRecordUseCase;
import edu.cuit.bc.evaluation.application.usecase.SubmitEvaluationUseCase;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaLogConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaRecordServiceImplTest {

    @Mock
    private EvaRecordPagingQueryPort evaRecordPagingQueryPort;

    @Mock
    private EvaRecordScoreQueryPort evaRecordScoreQueryPort;

    @Mock
    private EvaRecordBizConvertor evaRecordBizConvertor;

    @Spy
    private PaginationBizConvertor paginationBizConvertor = new PaginationBizConvertor();

    @Mock
    private SubmitEvaluationUseCase submitEvaluationUseCase;

    @Mock
    private DeleteEvaRecordUseCase deleteEvaRecordUseCase;

    @InjectMocks
    private EvaRecordServiceImpl service;

    @Test
    void pageEvaRecord_shouldFillAverageScoreFromRecord() {
        PagingQuery<EvaLogConditionalQuery> query = new PagingQuery<>();
        query.setPage(1);
        query.setSize(10);

        EvaRecordEntity record1 = new EvaRecordEntity();
        record1.setId(1);
        record1.setFormPropsValues("props-1");
        EvaRecordEntity record2 = new EvaRecordEntity();
        record2.setId(2);
        record2.setFormPropsValues("props-2");

        PaginationResultEntity<EvaRecordEntity> page = new PaginationResultEntity<EvaRecordEntity>()
                .setRecords(List.of(record1, record2))
                .setCurrent(1)
                .setSize(10)
                .setTotal(2);

        EvaRecordCO co1 = new EvaRecordCO().setId(1);
        EvaRecordCO co2 = new EvaRecordCO().setId(2);
        when(evaRecordPagingQueryPort.pageEvaRecord(1, query)).thenReturn(page);
        when(evaRecordBizConvertor.evaRecordEntityToCo(record1)).thenReturn(co1);
        when(evaRecordBizConvertor.evaRecordEntityToCo(record2)).thenReturn(co2);
        when(evaRecordScoreQueryPort.getScoreFromRecord("props-1")).thenReturn(Optional.of(80.0));
        when(evaRecordScoreQueryPort.getScoreFromRecord("props-2")).thenReturn(Optional.of(90.0));

        PaginationQueryResultCO<EvaRecordCO> result = service.pageEvaRecord(1, query);

        assertNotNull(result);
        assertEquals(2, result.getRecords().size());
        assertEquals(80.0, result.getRecords().get(0).getAverScore());
        assertEquals(90.0, result.getRecords().get(1).getAverScore());
        verify(evaRecordScoreQueryPort).getScoreFromRecord("props-1");
        verify(evaRecordScoreQueryPort).getScoreFromRecord("props-2");
    }

    @Test
    void pageEvaRecord_whenScoreMissing_shouldThrow() {
        PagingQuery<EvaLogConditionalQuery> query = new PagingQuery<>();
        query.setPage(1);
        query.setSize(10);

        EvaRecordEntity record = new EvaRecordEntity();
        record.setId(1);
        record.setFormPropsValues("props-1");

        PaginationResultEntity<EvaRecordEntity> page = new PaginationResultEntity<EvaRecordEntity>()
                .setRecords(List.of(record))
                .setCurrent(1)
                .setSize(10)
                .setTotal(1);

        EvaRecordCO co = new EvaRecordCO().setId(1);
        when(evaRecordPagingQueryPort.pageEvaRecord(1, query)).thenReturn(page);
        when(evaRecordBizConvertor.evaRecordEntityToCo(record)).thenReturn(co);
        when(evaRecordScoreQueryPort.getScoreFromRecord("props-1")).thenReturn(Optional.empty());

        SysException ex = assertThrows(SysException.class, () -> service.pageEvaRecord(1, query));
        assertEquals("相关模板不存在", ex.getMessage());
    }
}
