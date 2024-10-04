package edu.cuit.domain.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class PaginationResultEntity<T> {

    /**
     * 查询列表总记录
     */
    private List<T> records;

    /**
     * 查询总记录数
     */
    private Integer total;

    /**
     * 每页显示条数
     */
    private Integer size;

    /**
     * 当前页数
     */
    private Integer current;

}
