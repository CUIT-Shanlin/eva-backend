package edu.cuit.client.dto.query.condition;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 不合格用户条件查询
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UnqualifiedUserConditionalQuery extends ConditionalQuery{

    /**
     * 学院名称
     */
    private String department;

}
