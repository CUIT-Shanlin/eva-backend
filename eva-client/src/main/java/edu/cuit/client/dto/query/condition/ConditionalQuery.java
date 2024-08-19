package edu.cuit.client.dto.query.condition;

import com.alibaba.cola.dto.Query;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 条件查询根类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class ConditionalQuery extends Query {

    /**
     * 查询关键字
     */
    private String keyword;

}
