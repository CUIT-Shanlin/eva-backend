package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.dto.clientobject.DateEvaNumCO;
import edu.cuit.client.dto.clientobject.TimeEvaNumCO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 评教任务完成情况
 */

@EqualsAndHashCode(callSuper = false)
@Data
@Accessors(chain = true)
public class EvaSituationCO extends ClientObject{
    /**
     * evaNum 已评教个数
     */
    private Integer evaNum;
    /**
     * totalNum 总共需要评教的个数
     */
    private Integer totalNum;
    /**
     * moreNum 新增多少
     */
    private Integer moreNum;
    /**
     * moreEvaNum 新增多少待评教
     */
    private Integer moreEvaNum;
    /**
     * evaNumArr 7天内每日新增评教数目
     */
    private List<DateEvaNumCO> evaNumArr;
}
