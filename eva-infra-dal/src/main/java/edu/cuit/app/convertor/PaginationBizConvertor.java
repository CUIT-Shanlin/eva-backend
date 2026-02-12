package edu.cuit.app.convertor;

import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.domain.entity.PaginationResultEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分页业务对象转换器
 */
@Component
public class PaginationBizConvertor {

    public <T> PaginationQueryResultCO<T> toPaginationEntity(PaginationResultEntity<?> paginationResultEntity, List<T> values) {
        PaginationQueryResultCO<T> pageCO = new PaginationQueryResultCO<>();
        pageCO.setCurrent(paginationResultEntity.getCurrent())
                .setSize(paginationResultEntity.getSize())
                .setTotal(paginationResultEntity.getTotal())
                .setRecords(values);
        return pageCO;
    }

}
