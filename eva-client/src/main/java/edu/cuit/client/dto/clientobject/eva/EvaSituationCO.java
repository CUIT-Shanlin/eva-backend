package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评教任务完成情况
 */

@EqualsAndHashCode
@Data
@Accessors(chain = true)
public class EvaSituationCO {
    /**
     * id
     */
    private Long id;
    /**
     * evaNum 已评教个数
     */
    private Long evaNum;
    /**
     * totalNum 总共需要评教的个数
     */
    private Long totalNum;
    /**
     * moreNum 新增多少
     */
    private Long moreNum;
    /**
     * moreEvaNum 新增多少待评教
     */
    private Long moreEvaNum;
    /**
     * evaNumArr 7天内每日新增评教数目
     */
    private List<Integer> evaNumArr;
}
