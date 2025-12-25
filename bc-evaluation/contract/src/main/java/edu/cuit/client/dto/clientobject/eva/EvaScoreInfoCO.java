package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.dto.clientobject.SimplePercentCO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.util.List;

/**
 * 评教分数具体信息
 */
@EqualsAndHashCode(callSuper = false)
@Data
@Accessors(chain = true)
public class EvaScoreInfoCO extends ClientObject{
    /**
     * 低于 指定分数的数目
     */
    private Integer lowerNum;
    /**
     * 总记录数
     */
    private Integer totalNum;
    /**
     *高于指定分数的百分比
     */
    private Number percent;
    /**
     *较昨日 lowerNum 多了多少
     */
    private Integer moreNum;
    /**
     * 较昨日 percent 多了多少
     */
    private Number morePercent;
    /**
     * 7日内 percent 的值
     */
    private List<SimplePercentCO> percentArr;
}
