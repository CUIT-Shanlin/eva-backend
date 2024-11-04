package edu.cuit.client.dto.clientobject;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/**
 * 数据模型EvaSituationCO 需要
 * 这一天内评教次数的统计数据
 */
@EqualsAndHashCode(callSuper = false)
@Data
@Accessors(chain = true)
public class DateEvaNumCO extends ClientObject{

    /**
     * 新增评教数目moreEvaNum
     */
    private Integer value;
    /**
     * 当前时间点
     */
    private LocalDate date;
}
