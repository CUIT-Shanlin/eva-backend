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
public class OneDayAddEvaDataCO extends ClientObject{
    /**
     * 新增数目
     */
    private Integer moreNum;
    /**
     * 新增百分比率
     */
    private Number morePercent;
    /**
     *这一天内评教次数的统计数据
     */
    private List<TimeEvaNumCO> evaNumArr;
}
