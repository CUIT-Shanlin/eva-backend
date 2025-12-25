package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.dto.clientobject.TimeEvaNumCO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Data
@Accessors(chain = true)
public class EvaWeekAddCO extends ClientObject{
    /**
     * 新增数目
     */
    private Integer moreNum;
    /**
     * 新增百分比率
     */
    private Double morePercent;
    /**
     *这一天内评教次数的统计数据
     */
    private List<Integer> evaNumArr;
}
