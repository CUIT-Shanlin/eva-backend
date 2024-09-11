package edu.cuit.client.dto.clientobject;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = false)
@Data
@Accessors(chain = true)
public class SimplePercentCO extends ClientObject{
    /**
     * 数据对应日期
     */
    private String date;
    /**
     * 7日内 percent 的值
     */
    private Number value;
}
