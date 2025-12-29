package edu.cuit.bc.evaluation;

import com.alibaba.cola.exception.SysException;
import edu.cuit.bc.evaluation.application.port.EvaStatisticsOverviewQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaStatisticsTrendQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaStatisticsUnqualifiedUserQueryPort;
import edu.cuit.bc.evaluation.application.usecase.EvaStatisticsQueryUseCase;
import edu.cuit.client.dto.clientobject.eva.EvaScoreInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaSituationCO;
import edu.cuit.client.dto.clientobject.eva.EvaWeekAddCO;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaStatisticsQueryUseCaseTest {

    @Mock
    private EvaStatisticsOverviewQueryPort overviewQueryPort;

    @Mock
    private EvaStatisticsTrendQueryPort trendQueryPort;

    @Mock
    private EvaStatisticsUnqualifiedUserQueryPort unqualifiedUserQueryPort;

    @Mock
    private EvaConfigGateway evaConfigGateway;

    @Test
    void evaScoreStatisticsInfoOrEmpty_whenEmpty_shouldReturnEmptyObject() {
        EvaStatisticsQueryUseCase useCase = new EvaStatisticsQueryUseCase(
                overviewQueryPort,
                trendQueryPort,
                unqualifiedUserQueryPort,
                evaConfigGateway
        );

        when(overviewQueryPort.evaScoreStatisticsInfo(1, 60)).thenReturn(Optional.empty());

        EvaScoreInfoCO result = useCase.evaScoreStatisticsInfoOrEmpty(1, 60);

        assertNotNull(result);
        verify(overviewQueryPort).evaScoreStatisticsInfo(1, 60);
    }

    @Test
    void evaTemplateSituationOrEmpty_whenEmpty_shouldReturnEmptyObject() {
        EvaStatisticsQueryUseCase useCase = new EvaStatisticsQueryUseCase(
                overviewQueryPort,
                trendQueryPort,
                unqualifiedUserQueryPort,
                evaConfigGateway
        );

        when(overviewQueryPort.evaTemplateSituation(1)).thenReturn(Optional.empty());

        EvaSituationCO result = useCase.evaTemplateSituationOrEmpty(1);

        assertNotNull(result);
        verify(overviewQueryPort).evaTemplateSituation(1);
    }

    @Test
    void evaWeekAddOrEmpty_whenEmpty_shouldReturnEmptyObject() {
        EvaStatisticsQueryUseCase useCase = new EvaStatisticsQueryUseCase(
                overviewQueryPort,
                trendQueryPort,
                unqualifiedUserQueryPort,
                evaConfigGateway
        );

        when(trendQueryPort.evaWeekAdd(7, 1)).thenReturn(Optional.empty());

        EvaWeekAddCO result = useCase.evaWeekAddOrEmpty(7, 1);

        assertNotNull(result);
        verify(trendQueryPort).evaWeekAdd(7, 1);
    }

    @Test
    void pageUnqualifiedUser_type0_shouldUseMinEvaNum() {
        EvaStatisticsQueryUseCase useCase = new EvaStatisticsQueryUseCase(
                overviewQueryPort,
                trendQueryPort,
                unqualifiedUserQueryPort,
                evaConfigGateway
        );

        EvaConfigEntity config = new EvaConfigEntity();
        config.setMinEvaNum(4);
        config.setMinBeEvaNum(6);

        PagingQuery<UnqualifiedUserConditionalQuery> query = new PagingQuery<>();
        query.setPage(1);
        query.setSize(10);

        PaginationResultEntity<UnqualifiedUserInfoCO> page = new PaginationResultEntity<>();
        page.setRecords(List.of(new UnqualifiedUserInfoCO()));

        when(unqualifiedUserQueryPort.pageEvaUnqualifiedUserInfo(1, query, 4)).thenReturn(page);

        PaginationResultEntity<UnqualifiedUserInfoCO> result = useCase.pageUnqualifiedUser(1, 0, query, config);

        assertSame(page, result);
        verify(unqualifiedUserQueryPort).pageEvaUnqualifiedUserInfo(1, query, 4);
        verify(unqualifiedUserQueryPort, never()).pageBeEvaUnqualifiedUserInfo(any(), any(), any());
    }

    @Test
    void pageUnqualifiedUser_type1_shouldUseMinBeEvaNum() {
        EvaStatisticsQueryUseCase useCase = new EvaStatisticsQueryUseCase(
                overviewQueryPort,
                trendQueryPort,
                unqualifiedUserQueryPort,
                evaConfigGateway
        );

        EvaConfigEntity config = new EvaConfigEntity();
        config.setMinEvaNum(4);
        config.setMinBeEvaNum(6);

        PagingQuery<UnqualifiedUserConditionalQuery> query = new PagingQuery<>();
        query.setPage(1);
        query.setSize(10);

        PaginationResultEntity<UnqualifiedUserInfoCO> page = new PaginationResultEntity<>();
        page.setRecords(List.of(new UnqualifiedUserInfoCO()));

        when(unqualifiedUserQueryPort.pageBeEvaUnqualifiedUserInfo(1, query, 6)).thenReturn(page);

        PaginationResultEntity<UnqualifiedUserInfoCO> result = useCase.pageUnqualifiedUser(1, 1, query, config);

        assertSame(page, result);
        verify(unqualifiedUserQueryPort).pageBeEvaUnqualifiedUserInfo(1, query, 6);
        verify(unqualifiedUserQueryPort, never()).pageEvaUnqualifiedUserInfo(any(), any(), any());
    }

    @Test
    void pageUnqualifiedUser_invalidType_shouldThrow() {
        EvaStatisticsQueryUseCase useCase = new EvaStatisticsQueryUseCase(
                overviewQueryPort,
                trendQueryPort,
                unqualifiedUserQueryPort,
                evaConfigGateway
        );

        EvaConfigEntity config = new EvaConfigEntity();
        config.setMinEvaNum(4);
        config.setMinBeEvaNum(6);

        PagingQuery<UnqualifiedUserConditionalQuery> query = new PagingQuery<>();
        query.setPage(1);
        query.setSize(10);

        SysException ex = assertThrows(SysException.class, () -> useCase.pageUnqualifiedUser(1, 2, query, config));
        assertEquals("type是10以外的值", ex.getMessage());
    }

    @Test
    void getEvaData_shouldUseConfiguredThresholdsInOrder() {
        EvaStatisticsQueryUseCase useCase = new EvaStatisticsQueryUseCase(
                overviewQueryPort,
                trendQueryPort,
                unqualifiedUserQueryPort,
                evaConfigGateway
        );

        when(evaConfigGateway.getMinEvaNum()).thenReturn(3);
        when(evaConfigGateway.getMinBeEvaNum()).thenReturn(5);
        PastTimeEvaDetailCO detail = new PastTimeEvaDetailCO();
        when(overviewQueryPort.getEvaData(1, 7, 3, 5)).thenReturn(Optional.of(detail));

        Optional<PastTimeEvaDetailCO> result = useCase.getEvaData(1, 7);

        assertSame(detail, result.orElseThrow());
        var inOrder = inOrder(evaConfigGateway, overviewQueryPort);
        inOrder.verify(evaConfigGateway).getMinEvaNum();
        inOrder.verify(evaConfigGateway).getMinBeEvaNum();
        inOrder.verify(overviewQueryPort).getEvaData(1, 7, 3, 5);
    }

    @Test
    void pageUnqualifiedUser_overload_shouldLoadConfigAndDelegate() {
        EvaStatisticsQueryUseCase useCase = new EvaStatisticsQueryUseCase(
                overviewQueryPort,
                trendQueryPort,
                unqualifiedUserQueryPort,
                evaConfigGateway
        );

        EvaConfigEntity config = new EvaConfigEntity();
        config.setMinEvaNum(4);
        config.setMinBeEvaNum(6);
        when(evaConfigGateway.getEvaConfig()).thenReturn(config);

        PagingQuery<UnqualifiedUserConditionalQuery> query = new PagingQuery<>();
        query.setPage(1);
        query.setSize(10);

        PaginationResultEntity<UnqualifiedUserInfoCO> page = new PaginationResultEntity<>();
        page.setRecords(List.of(new UnqualifiedUserInfoCO()));
        when(unqualifiedUserQueryPort.pageEvaUnqualifiedUserInfo(1, query, 4)).thenReturn(page);

        PaginationResultEntity<UnqualifiedUserInfoCO> result = useCase.pageUnqualifiedUser(1, 0, query);

        assertSame(page, result);
        var inOrder = inOrder(evaConfigGateway, unqualifiedUserQueryPort);
        inOrder.verify(evaConfigGateway).getEvaConfig();
        inOrder.verify(unqualifiedUserQueryPort).pageEvaUnqualifiedUserInfo(1, query, 4);
    }

    @Test
    void getTargetAmountUnqualifiedUser_overload_type0_whenEmpty_shouldReturnError() {
        EvaStatisticsQueryUseCase useCase = new EvaStatisticsQueryUseCase(
                overviewQueryPort,
                trendQueryPort,
                unqualifiedUserQueryPort,
                evaConfigGateway
        );

        EvaConfigEntity config = new EvaConfigEntity();
        config.setMinEvaNum(3);
        config.setMinBeEvaNum(5);
        when(evaConfigGateway.getEvaConfig()).thenReturn(config);
        when(unqualifiedUserQueryPort.getEvaTargetAmountUnqualifiedUser(1, 7, 3)).thenReturn(Optional.empty());

        UnqualifiedUserResultCO error = new UnqualifiedUserResultCO();
        UnqualifiedUserResultCO result = useCase.getTargetAmountUnqualifiedUser(1, 0, 7, error);

        assertSame(error, result);
        var inOrder = inOrder(evaConfigGateway, unqualifiedUserQueryPort);
        inOrder.verify(evaConfigGateway).getEvaConfig();
        inOrder.verify(unqualifiedUserQueryPort).getEvaTargetAmountUnqualifiedUser(1, 7, 3);
    }
}
