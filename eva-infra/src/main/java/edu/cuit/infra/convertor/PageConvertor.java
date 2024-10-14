package edu.cuit.infra.convertor;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;

import java.io.Serializable;
import java.util.List;

public class PageConvertor {
    public static PaginationQueryResultCO convert(Page page)
    {
        PaginationQueryResultCO result = new PaginationQueryResultCO<>();
        result.setCurrent((int) page.getCurrent());
        result.setSize((int) page.getSize());
        result.setTotal((int) page.getTotal());
        result.setRecords(page.getRecords());
        return result;
    }
}
