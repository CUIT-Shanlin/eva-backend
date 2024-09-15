package edu.cuit.domain.entity.eva;

import com.alibaba.cola.domain.Entity;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评教记录domain entity
 */
@Entity
@Data
@RequiredArgsConstructor
public class EvaRecordEntity {

    /**
     * id
     */
    private Integer id;

    /**
     * 评教任务
     */
    private EvaTaskEntity task;

    /**
     * 详细的文字评价
     */
    private String textValue;

    /**
     * 表单评教指标以及对应的分值，JSON字符串格式的对象数组
     */
    private String formPropsValues;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 实现逻辑删除（0:不可用 1:可用）
     */
    private Integer isDeleted;

}
