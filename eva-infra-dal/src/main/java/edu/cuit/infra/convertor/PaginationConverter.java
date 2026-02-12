package edu.cuit.infra.convertor;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.cuit.domain.entity.PaginationResultEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页对象转换器
 */
@Component
public class PaginationConverter {

    public <T> PaginationResultEntity<T> toPaginationEntity(Page<?> page, List<T> values) {
        PaginationResultEntity<T> entity = new PaginationResultEntity<>();
        return entity.setRecords(new ArrayList<>(values))
                .setSize((int) page.getSize())
                .setCurrent((int) page.getCurrent())
                .setTotal((int) page.getTotal());
    }

}
