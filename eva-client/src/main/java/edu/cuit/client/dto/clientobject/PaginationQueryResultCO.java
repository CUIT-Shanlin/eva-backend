package edu.cuit.client.dto.clientobject;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;

@Data
@Accessors(chain = true)
public class PaginationQueryResultCO extends ClientObject {
    /**
     * 查询列表总记录
     */
    private List<Object> records;

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
