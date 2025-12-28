package edu.cuit.app.eva;

import com.alibaba.cola.exception.SysException;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.app.service.impl.eva.EvaStatisticsServiceImpl;
import edu.cuit.bc.evaluation.application.usecase.EvaStatisticsQueryUseCase;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.PastTimeEvaDetailCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.UnqualifiedUserConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaConfigEntity;
import edu.cuit.domain.gateway.eva.EvaConfigGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaStatisticsServiceImplTest {

    @Mock
    private EvaStatisticsQueryUseCase evaStatisticsQueryUseCase;

    @Mock
    private PaginationBizConvertor paginationBizConvertor;

    @Mock
    private EvaConfigGateway evaConfigGateway;

    @InjectMocks
    private EvaStatisticsServiceImpl service;

    @Test
    void getEvaData_shouldUseConfiguredThresholds() {
        PastTimeEvaDetailCO detail = new PastTimeEvaDetailCO();
        when(evaStatisticsQueryUseCase.getEvaData(1, 7)).thenReturn(Optional.of(detail));

        PastTimeEvaDetailCO result = service.getEvaData(1, 7);

        assertSame(detail, result);
        verify(evaStatisticsQueryUseCase).getEvaData(1, 7);
        verify(evaConfigGateway, never()).getMinEvaNum();
        verify(evaConfigGateway, never()).getMinBeEvaNum();
    }

    @Test
    void pageUnqualifiedUser_type0_shouldUseMinEvaNum() {
        EvaConfigEntity config = new EvaConfigEntity();
        config.setMinEvaNum(4);
        config.setMinBeEvaNum(6);
        when(evaConfigGateway.getEvaConfig()).thenReturn(config);

        PagingQuery<UnqualifiedUserConditionalQuery> query = new PagingQuery<>();
        query.setPage(1);
        query.setSize(10);

        PaginationResultEntity<UnqualifiedUserInfoCO> page = new PaginationResultEntity<>();
        page.setRecords(List.of(new UnqualifiedUserInfoCO()));

        PaginationQueryResultCO<UnqualifiedUserInfoCO> expected = new PaginationQueryResultCO<>();
        when(evaStatisticsQueryUseCase.pageUnqualifiedUser(1, 0, query, config)).thenReturn(page);
        when(paginationBizConvertor.toPaginationEntity(page, page.getRecords())).thenReturn(expected);

        PaginationQueryResultCO<UnqualifiedUserInfoCO> result = service.pageUnqualifiedUser(1, 0, query);

        assertSame(expected, result);
        verify(evaStatisticsQueryUseCase).pageUnqualifiedUser(1, 0, query, config);
    }

    @Test
    void pageUnqualifiedUser_type1_shouldUseMinBeEvaNum() {
        EvaConfigEntity config = new EvaConfigEntity();
        config.setMinEvaNum(4);
        config.setMinBeEvaNum(6);
        when(evaConfigGateway.getEvaConfig()).thenReturn(config);

        PagingQuery<UnqualifiedUserConditionalQuery> query = new PagingQuery<>();
        query.setPage(1);
        query.setSize(10);

        PaginationResultEntity<UnqualifiedUserInfoCO> page = new PaginationResultEntity<>();
        page.setRecords(List.of(new UnqualifiedUserInfoCO()));

        PaginationQueryResultCO<UnqualifiedUserInfoCO> expected = new PaginationQueryResultCO<>();
        when(evaStatisticsQueryUseCase.pageUnqualifiedUser(1, 1, query, config)).thenReturn(page);
        when(paginationBizConvertor.toPaginationEntity(page, page.getRecords())).thenReturn(expected);

        PaginationQueryResultCO<UnqualifiedUserInfoCO> result = service.pageUnqualifiedUser(1, 1, query);

        assertSame(expected, result);
        verify(evaStatisticsQueryUseCase).pageUnqualifiedUser(1, 1, query, config);
    }

    @Test
    void pageUnqualifiedUser_invalidType_shouldThrow() {
        EvaConfigEntity config = new EvaConfigEntity();
        when(evaConfigGateway.getEvaConfig()).thenReturn(config);
        when(evaStatisticsQueryUseCase.pageUnqualifiedUser(eq(1), eq(2), any(), eq(config)))
                .thenThrow(new SysException("type是10以外的值"));

        PagingQuery<UnqualifiedUserConditionalQuery> query = new PagingQuery<>();
        query.setPage(1);
        query.setSize(10);

        SysException ex = assertThrows(SysException.class, () -> service.pageUnqualifiedUser(1, 2, query));
        assertEquals("type是10以外的值", ex.getMessage());
    }

    @Test
    void getTargetAmountUnqualifiedUser_type0_whenEmpty_shouldReturnZeroResult() {
        EvaConfigEntity config = new EvaConfigEntity();
        config.setMinEvaNum(3);
        config.setMinBeEvaNum(5);
        when(evaConfigGateway.getEvaConfig()).thenReturn(config);
        when(evaStatisticsQueryUseCase.getTargetAmountUnqualifiedUser(eq(1), eq(0), eq(5), eq(config), any()))
                .thenAnswer(invocation -> invocation.getArgument(4));

        UnqualifiedUserResultCO result = service.getTargetAmountUnqualifiedUser(1, 0, 5);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertNotNull(result.getDataArr());
        assertTrue(result.getDataArr().isEmpty());
    }

    @Test
    void getTargetAmountUnqualifiedUser_type1_shouldUseMinBeEvaNum() {
        EvaConfigEntity config = new EvaConfigEntity();
        config.setMinEvaNum(3);
        config.setMinBeEvaNum(5);
        when(evaConfigGateway.getEvaConfig()).thenReturn(config);
        UnqualifiedUserResultCO expected = new UnqualifiedUserResultCO();
        when(evaStatisticsQueryUseCase.getTargetAmountUnqualifiedUser(eq(1), eq(1), eq(2), eq(config), any()))
                .thenReturn(expected);

        UnqualifiedUserResultCO result = service.getTargetAmountUnqualifiedUser(1, 1, 2);

        assertSame(expected, result);
        verify(evaStatisticsQueryUseCase).getTargetAmountUnqualifiedUser(eq(1), eq(1), eq(2), eq(config), any());
    }

    @Test
    void getTargetAmountUnqualifiedUser_invalidType_shouldThrow() {
        EvaConfigEntity config = new EvaConfigEntity();
        when(evaConfigGateway.getEvaConfig()).thenReturn(config);
        when(evaStatisticsQueryUseCase.getTargetAmountUnqualifiedUser(eq(1), eq(3), eq(5), eq(config), any()))
                .thenThrow(new SysException("type是10以外的值"));

        SysException ex = assertThrows(SysException.class, () -> service.getTargetAmountUnqualifiedUser(1, 3, 5));
        assertEquals("type是10以外的值", ex.getMessage());
    }
}
