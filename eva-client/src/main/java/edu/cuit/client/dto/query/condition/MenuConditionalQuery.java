package edu.cuit.client.dto.query.condition;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 树形菜单条件查询
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MenuConditionalQuery extends ConditionalQuery{

    /**
     * 状态
     */
    @Min(value = 0,message = "状态值只能为0和1")
    @Max(value = 1,message = "状态值只能为0和1")
    private Integer status;

}
