package edu.cuit.client.dto.clientobject.user;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 未达标用户模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class UnqualifiedUserInfoCO extends ClientObject {

    /**
     * 用户id
     */
    private Integer id;

    /**
     * 用户姓名
     */
    private String name;

    /**
     * 学院名称
     */
    private String department;

    /**
     * 已经完成的评教数目
     */
    private Integer num;

}
