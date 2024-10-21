package edu.cuit.client.dto.clientobject;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 数据模型EvaSituationCO 需要
 * 这一天内评教次数的统计数据
 */
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
    private String time;
}
