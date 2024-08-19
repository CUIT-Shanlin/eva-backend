package edu.cuit.client.dto.query.condition;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 条件查询模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GenericConditionalQuery extends ConditionalQuery {

    /**
     * 按照创建时间搜索的左边界
     */
    private LocalDateTime startCreateTime;

    /**
     * 按照创建时间搜索的右边界
     */
    private LocalDateTime endCreateTime;

    /**
     * 按照修改时间搜索的左边界
     */
    private LocalDateTime startUpdateTime;

    /**
     * 按照修改时间搜索的右边界
     */
    private LocalDateTime endUpdateTime;
}
