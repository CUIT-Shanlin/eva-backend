package edu.cuit.app.eva;

import com.alibaba.cola.exception.SysException;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.app.service.impl.eva.EvaStatisticsServiceImpl;
import edu.cuit.bc.evaluation.application.usecase.EvaStatisticsQueryUseCase;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaScoreInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaSituationCO;
import edu.cuit.client.dto.clientobject.eva.EvaWeekAddCO;
import edu.cuit.client.dto.clientobject.eva.PastTimeEvaDetailCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.UnqualifiedUserConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
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

    @InjectMocks
    private EvaStatisticsServiceImpl service;

    @Test
    void getEvaData_shouldUseConfiguredThresholds() {
        PastTimeEvaDetailCO detail = new PastTimeEvaDetailCO();
        when(evaStatisticsQueryUseCase.getEvaDataOrEmpty(1, 7)).thenReturn(detail);

        PastTimeEvaDetailCO result = service.getEvaData(1, 7);

        assertSame(detail, result);
        verify(evaStatisticsQueryUseCase).getEvaDataOrEmpty(1, 7);
    }

    @Test
    void evaScoreStatisticsInfo_shouldDelegateToUseCaseOrEmpty() {
        EvaScoreInfoCO expected = new EvaScoreInfoCO();
        when(evaStatisticsQueryUseCase.evaScoreStatisticsInfoOrEmpty(1, 60)).thenReturn(expected);

        EvaScoreInfoCO result = service.evaScoreStatisticsInfo(1, 60);

        assertSame(expected, result);
        verify(evaStatisticsQueryUseCase).evaScoreStatisticsInfoOrEmpty(1, 60);
    }

    @Test
    void evaTemplateSituation_shouldDelegateToUseCaseOrEmpty() {
        EvaSituationCO expected = new EvaSituationCO();
        when(evaStatisticsQueryUseCase.evaTemplateSituationOrEmpty(1)).thenReturn(expected);

        EvaSituationCO result = service.evaTemplateSituation(1);

        assertSame(expected, result);
        verify(evaStatisticsQueryUseCase).evaTemplateSituationOrEmpty(1);
    }

    @Test
    void evaWeekAdd_shouldDelegateToUseCaseOrEmpty() {
        EvaWeekAddCO expected = new EvaWeekAddCO();
        when(evaStatisticsQueryUseCase.evaWeekAddOrEmpty(7, 1)).thenReturn(expected);

        EvaWeekAddCO result = service.evaWeekAdd(7, 1);

        assertSame(expected, result);
        verify(evaStatisticsQueryUseCase).evaWeekAddOrEmpty(7, 1);
    }

    @Test
    void pageUnqualifiedUser_type0_shouldUseMinEvaNum() {
        PagingQuery<UnqualifiedUserConditionalQuery> query = new PagingQuery<>();
        query.setPage(1);
        query.setSize(10);

        PaginationResultEntity<UnqualifiedUserInfoCO> page = new PaginationResultEntity<>();
        page.setRecords(List.of(new UnqualifiedUserInfoCO()));

        PaginationQueryResultCO<UnqualifiedUserInfoCO> expected = new PaginationQueryResultCO<>();
        when(evaStatisticsQueryUseCase.pageUnqualifiedUser(1, 0, query)).thenReturn(page);
        when(paginationBizConvertor.toPaginationEntity(page, page.getRecords())).thenReturn(expected);

        PaginationQueryResultCO<UnqualifiedUserInfoCO> result = service.pageUnqualifiedUser(1, 0, query);

        assertSame(expected, result);
        verify(evaStatisticsQueryUseCase).pageUnqualifiedUser(1, 0, query);
    }

    @Test
    void pageUnqualifiedUser_type1_shouldUseMinBeEvaNum() {
        PagingQuery<UnqualifiedUserConditionalQuery> query = new PagingQuery<>();
        query.setPage(1);
        query.setSize(10);

        PaginationResultEntity<UnqualifiedUserInfoCO> page = new PaginationResultEntity<>();
        page.setRecords(List.of(new UnqualifiedUserInfoCO()));

        PaginationQueryResultCO<UnqualifiedUserInfoCO> expected = new PaginationQueryResultCO<>();
        when(evaStatisticsQueryUseCase.pageUnqualifiedUser(1, 1, query)).thenReturn(page);
        when(paginationBizConvertor.toPaginationEntity(page, page.getRecords())).thenReturn(expected);

        PaginationQueryResultCO<UnqualifiedUserInfoCO> result = service.pageUnqualifiedUser(1, 1, query);

        assertSame(expected, result);
        verify(evaStatisticsQueryUseCase).pageUnqualifiedUser(1, 1, query);
    }

    @Test
    void pageUnqualifiedUser_invalidType_shouldThrow() {
        when(evaStatisticsQueryUseCase.pageUnqualifiedUser(eq(1), eq(2), any()))
                .thenThrow(new SysException("type是10以外的值"));

        PagingQuery<UnqualifiedUserConditionalQuery> query = new PagingQuery<>();
        query.setPage(1);
        query.setSize(10);

        SysException ex = assertThrows(SysException.class, () -> service.pageUnqualifiedUser(1, 2, query));
        assertEquals("type是10以外的值", ex.getMessage());
    }

    @Test
    void getTargetAmountUnqualifiedUser_type0_whenEmpty_shouldReturnZeroResult() {
        when(evaStatisticsQueryUseCase.getTargetAmountUnqualifiedUser(eq(1), eq(0), eq(5), any()))
                .thenAnswer(invocation -> invocation.getArgument(3));

        UnqualifiedUserResultCO result = service.getTargetAmountUnqualifiedUser(1, 0, 5);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertNotNull(result.getDataArr());
        assertTrue(result.getDataArr().isEmpty());
    }

    @Test
    void getTargetAmountUnqualifiedUser_type1_shouldUseMinBeEvaNum() {
        UnqualifiedUserResultCO expected = new UnqualifiedUserResultCO();
        when(evaStatisticsQueryUseCase.getTargetAmountUnqualifiedUser(eq(1), eq(1), eq(2), any()))
                .thenReturn(expected);

        UnqualifiedUserResultCO result = service.getTargetAmountUnqualifiedUser(1, 1, 2);

        assertSame(expected, result);
        verify(evaStatisticsQueryUseCase).getTargetAmountUnqualifiedUser(eq(1), eq(1), eq(2), any());
    }

    @Test
    void getTargetAmountUnqualifiedUser_invalidType_shouldThrow() {
        when(evaStatisticsQueryUseCase.getTargetAmountUnqualifiedUser(eq(1), eq(3), eq(5), any()))
                .thenThrow(new SysException("type是10以外的值"));

        SysException ex = assertThrows(SysException.class, () -> service.getTargetAmountUnqualifiedUser(1, 3, 5));
        assertEquals("type是10以外的值", ex.getMessage());
    }
}
