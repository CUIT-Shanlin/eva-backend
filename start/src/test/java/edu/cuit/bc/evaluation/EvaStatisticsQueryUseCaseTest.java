package edu.cuit.bc.evaluation;

import com.alibaba.cola.exception.SysException;
import edu.cuit.bc.evaluation.application.port.EvaStatisticsOverviewQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaStatisticsTrendQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaStatisticsUnqualifiedUserQueryPort;
import edu.cuit.bc.evaluation.application.usecase.EvaStatisticsQueryUseCase;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.UnqualifiedUserConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaConfigEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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

    @Test
    void pageUnqualifiedUser_type0_shouldUseMinEvaNum() {
        EvaStatisticsQueryUseCase useCase = new EvaStatisticsQueryUseCase(overviewQueryPort, trendQueryPort, unqualifiedUserQueryPort);

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
        EvaStatisticsQueryUseCase useCase = new EvaStatisticsQueryUseCase(overviewQueryPort, trendQueryPort, unqualifiedUserQueryPort);

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
        EvaStatisticsQueryUseCase useCase = new EvaStatisticsQueryUseCase(overviewQueryPort, trendQueryPort, unqualifiedUserQueryPort);

        EvaConfigEntity config = new EvaConfigEntity();
        config.setMinEvaNum(4);
        config.setMinBeEvaNum(6);

        PagingQuery<UnqualifiedUserConditionalQuery> query = new PagingQuery<>();
        query.setPage(1);
        query.setSize(10);

        SysException ex = assertThrows(SysException.class, () -> useCase.pageUnqualifiedUser(1, 2, query, config));
        assertEquals("type是10以外的值", ex.getMessage());
    }
}

