package edu.cuit.client.dto.clientobject;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 过去一段时间内的详细评教统计数据，后端=》前端，
 */
@EqualsAndHashCode(callSuper = false)
@Data
@Accessors(chain = true)
public class SimpleEvaPercentCO extends ClientObject{
    /**
     * 评教总次数
     */
    private Integer num;
    /**
     * num较再上次统计增加多少百分比
     */
    private Number morePercent;
}
