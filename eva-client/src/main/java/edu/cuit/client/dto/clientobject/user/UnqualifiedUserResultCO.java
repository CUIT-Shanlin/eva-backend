package edu.cuit.client.dto.clientobject.user;

import com.alibaba.cola.dto.ClientObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取指定数量未达标用户结果模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@NoArgsConstructor
public class UnqualifiedUserResultCO extends ClientObject {

    /**
     * 响应用户数据
     */
    private List<UnqualifiedUserInfoCO> dataArr;

    /**
     * 总共多少未达标的人
     */
    private Integer total;

    public UnqualifiedUserResultCO(List<UnqualifiedUserInfoCO> data) {
        this.dataArr = new ArrayList<>(data);
        total = data.size();
    }

}
