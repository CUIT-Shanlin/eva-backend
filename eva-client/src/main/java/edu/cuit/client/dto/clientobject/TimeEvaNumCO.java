package edu.cuit.client.dto.clientobject;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalTime;

@EqualsAndHashCode(callSuper = false)
@Data
@Accessors(chain = true)
public class TimeEvaNumCO extends ClientObject{
    /**
     * 新增评教数目
     */
    private Integer moreEvaNum;
    /**
     * 当前时间点
     */
    private LocalTime time;
}
