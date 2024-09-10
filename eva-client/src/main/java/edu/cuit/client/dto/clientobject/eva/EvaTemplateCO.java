package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 评教表单模版
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class EvaTemplateCO extends ClientObject {

    /**
     * 模版id
     */
    private Integer id;

    /**
     *模板名称
     */
    private String name;

    /**
     *描述
     */
    private String description;

    /**
     * 表单评教指标，JSON表示的字符串形式
     */
    private String props;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;





}
