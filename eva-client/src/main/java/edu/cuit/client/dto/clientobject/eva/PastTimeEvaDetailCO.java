package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.dto.clientobject.SimpleEvaPercentCO;
import edu.cuit.client.dto.clientobject.TimeEvaNumCO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Data
@Accessors(chain = true)
public class PastTimeEvaDetailCO extends ClientObject{
    /**
     * 评教总体的数据信息
     */
    private SimpleEvaPercentCO totalEvaInfo;
    /**
     * 评教达标方面的数据
     */
    private SimpleEvaPercentCO evaQualifiedInfo;
    /**
     * 被评教达标方面的数据
     */
    private SimpleEvaPercentCO qualifiedInfo;
    /**
     * 指定时间段内每天的新增评教数目
     */
    private List<TimeEvaNumCO> dataArr;
}
