package edu.cuit.client.dto.clientobject;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 分页数据响应模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class PaginationQueryResultCO<T extends Serializable> extends ClientObject {

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
