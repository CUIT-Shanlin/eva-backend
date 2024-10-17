package edu.cuit.app.service.impl.eva;

import com.alibaba.cola.exception.BizException;
import edu.cuit.app.convertor.user.MenuBizConvertor;
import edu.cuit.client.api.eva.IEvaRecordService;
import edu.cuit.client.api.user.IMenuService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskFormCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaLogConditionalQuery;
import edu.cuit.client.dto.query.condition.MenuConditionalQuery;
import edu.cuit.domain.gateway.eva.EvaDeleteGateway;
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
import edu.cuit.domain.gateway.eva.EvaUpdateGateway;
import edu.cuit.domain.gateway.user.MenuQueryGateway;
import edu.cuit.domain.gateway.user.MenuUpdateGateway;
import edu.cuit.infra.convertor.eva.EvaConvertor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaRecordServiceImpl implements IEvaRecordService {
    private final EvaDeleteGateway evaDeleteGateway;
    private final EvaUpdateGateway evaUpdateGateway;
    private final EvaQueryGateway evaQueryGateway;
    private final EvaConvertor convertor;
    @Override
    public PaginationQueryResultCO<EvaRecordCO> pageEvaRecord(Integer semId, PagingQuery<EvaLogConditionalQuery> query) {
        return null;
    }

    @Override
    public Void deleteOneEvaLogById(Integer id) {
        return null;
    }

    @Override
    public Void deleteEvaLogsById(List<Integer> ids) {
        return null;
    }

    @Override
    public Void putEvaTemplate(EvaTaskFormCO evaTaskFormCO) {
        return null;
    }
}
