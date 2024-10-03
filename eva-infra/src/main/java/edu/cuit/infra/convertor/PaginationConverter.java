package edu.cuit.infra.convertor;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.cuit.domain.entity.PaginationResultEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * 分页对象转换器
 */
@Mapper(componentModel = "spring",uses = EntityFactory.class)
public interface PaginationConverter {

    @Mappings({
            @Mapping(target = "records",source = "values"),
            @Mapping(target = "total",source = "page.total"),
            @Mapping(target = "size",source = "page.size"),
            @Mapping(target = "current",source = "page.current")
    })
    <T> PaginationResultEntity<T> toPaginationEntity(Page<?> page, List<T> values);

}
