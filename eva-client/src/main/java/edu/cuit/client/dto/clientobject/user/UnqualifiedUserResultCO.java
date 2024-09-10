package edu.cuit.client.dto.clientobject.user;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 获取指定数量未达标用户结果模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class UnqualifiedUserResultCO extends ClientObject {

    /**
     * 响应用户数据
     */
    private List<UnqualifiedUserInfoCO> dataArr;

    /**
     * 总共多少未达标的人
     */
    private Integer total;

}
