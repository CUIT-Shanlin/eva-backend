package edu.cuit.client.dto.clientobject;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@EqualsAndHashCode(callSuper = false)
@Data
@Accessors(chain = true)
public class SimplePercentCO extends ClientObject{
    /**
     * 数据对应日期
     */
    private LocalDate date;
    /**
     * 7日内 percent 的值
     */
    private Double value;

}
