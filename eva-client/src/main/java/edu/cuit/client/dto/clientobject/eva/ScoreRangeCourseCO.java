package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 接口-评教相关-统计-获取各个分数段中-课程的数目情况 需要
 * 一个分数段内的课程数目
 */

@EqualsAndHashCode(callSuper = false)
@Data
@Accessors(chain = true)
public class ScoreRangeCourseCO extends ClientObject{
    /**
     * id
     */
    private Long id;
    /**
     * 该分数段的起始分数
     */
    private Integer startScore;
    /**
     * 该分数段的截止分数
     */
    private Integer endScore;
    /**
     *该分数段有几门课
     */
    private Number count;
}
