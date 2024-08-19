package edu.cuit.client.dto.query;

import com.alibaba.cola.dto.Query;
import edu.cuit.client.dto.query.condition.ConditionalQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分页查询请求模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PagingQuery extends Query {

    /**
     * 页数
     */
    @Min(value = 1,message = "页数不能少于1")
    @NotNull(message = "页数不能为空")
    private Integer page;

    /**
     * 一页的数据个数
     */
    @Min(value = 1,message = "元素个数不能少于1")
    @NotNull(message = "元素个数(size)不能为空")
    private Integer size;

    /**
     * 查询条件对象
     */
    @Valid
    private ConditionalQuery queryObj;
}
