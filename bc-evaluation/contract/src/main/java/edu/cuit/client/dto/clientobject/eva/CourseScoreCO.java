package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 一门课的评教分数
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class CourseScoreCO extends ClientObject {

    /**
     * 平均分
     */
    private Double averScore;

    /**
     * 最高分
     */
    private Double maxScore;

    /**
     * 最低分
     */
    private Double minScore;

    /**
     * 指标，指标字符串
     */
    private String prop;

}
