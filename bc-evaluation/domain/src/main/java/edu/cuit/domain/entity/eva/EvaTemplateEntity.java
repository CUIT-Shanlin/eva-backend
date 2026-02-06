package edu.cuit.domain.entity.eva;

import com.alibaba.cola.domain.Entity;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评教模板domain entity
 */
@Entity
@Data
@RequiredArgsConstructor
public class EvaTemplateEntity {

    /**
     * 模板id
     */
    private Integer id;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 表单评教指标，JSON表示的字符串形式
     */
    private String props;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 实现逻辑删除（0:不可用 1:可用）
     */
    private Integer isDeleted;

    /**
     * 判断该数据是否是默认数据，0: 理论课相关默认；1: 实验课相关默认；-1：非默认数据
     */
    private Integer isDefault;

}
