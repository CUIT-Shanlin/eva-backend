package edu.cuit.app.eva;

import com.alibaba.cola.exception.SysException;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.app.service.impl.eva.EvaStatisticsServiceImpl;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.PastTimeEvaDetailCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.UnqualifiedUserConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaConfigEntity;
import edu.cuit.domain.gateway.eva.EvaConfigGateway;
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
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
    private EvaQueryGateway evaQueryGateway;

    @Mock
    private PaginationBizConvertor paginationBizConvertor;

    @Mock
    private EvaConfigGateway evaConfigGateway;

    @InjectMocks
    private EvaStatisticsServiceImpl service;

    @Test
    void getEvaData_shouldUseConfiguredThresholds() {
        when(evaConfigGateway.getMinEvaNum()).thenReturn(3);
        when(evaConfigGateway.getMinBeEvaNum()).thenReturn(5);
        PastTimeEvaDetailCO detail = new PastTimeEvaDetailCO();
        when(evaQueryGateway.getEvaData(1, 7, 3, 5)).thenReturn(Optional.of(detail));

        PastTimeEvaDetailCO result = service.getEvaData(1, 7);

        assertSame(detail, result);
        verify(evaQueryGateway).getEvaData(1, 7, 3, 5);
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
        when(evaQueryGateway.pageEvaUnqualifiedUserInfo(1, query, 4)).thenReturn(page);
        when(paginationBizConvertor.toPaginationEntity(page, page.getRecords())).thenReturn(expected);

        PaginationQueryResultCO<UnqualifiedUserInfoCO> result = service.pageUnqualifiedUser(1, 0, query);

        assertSame(expected, result);
        verify(evaQueryGateway).pageEvaUnqualifiedUserInfo(1, query, 4);
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
        when(evaQueryGateway.pageBeEvaUnqualifiedUserInfo(1, query, 6)).thenReturn(page);
        when(paginationBizConvertor.toPaginationEntity(page, page.getRecords())).thenReturn(expected);

        PaginationQueryResultCO<UnqualifiedUserInfoCO> result = service.pageUnqualifiedUser(1, 1, query);

        assertSame(expected, result);
        verify(evaQueryGateway).pageBeEvaUnqualifiedUserInfo(1, query, 6);
    }

    @Test
    void pageUnqualifiedUser_invalidType_shouldThrow() {
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
        when(evaQueryGateway.getEvaTargetAmountUnqualifiedUser(1, 5, 3)).thenReturn(Optional.empty());

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
        when(evaQueryGateway.getBeEvaTargetAmountUnqualifiedUser(1, 2, 5)).thenReturn(Optional.of(expected));

        UnqualifiedUserResultCO result = service.getTargetAmountUnqualifiedUser(1, 1, 2);

        assertSame(expected, result);
        verify(evaQueryGateway).getBeEvaTargetAmountUnqualifiedUser(1, 2, 5);
    }

    @Test
    void getTargetAmountUnqualifiedUser_invalidType_shouldThrow() {
        SysException ex = assertThrows(SysException.class, () -> service.getTargetAmountUnqualifiedUser(1, 3, 5));
        assertEquals("type是10以外的值", ex.getMessage());
    }
}
