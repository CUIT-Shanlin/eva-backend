package edu.cuit.client.bo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 系统日志业务类
 */
@Data
@Accessors(chain = true)
public class SysLogBO {

    /**
     * 用户编号
     */
    private String userId;

    /**
     * 用户IP
     */
    private String ip;

    /**
     * 模块id
     */
    private Integer moduleId;

    /**
     * 操作分类
     */
    private Integer type;

    /**
     * 操作明细
     */
    private String content;


}
