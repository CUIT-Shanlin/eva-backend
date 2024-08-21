package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.util.List;

/**
 * 用户的各个课程的评分,对象数组形式，后端=》前端
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class EvaCourseScoreCO extends ClientObject {
    /**
     * 用户的各个课程的评分
     */
    private List<EvaLogCO> coursescore;
}
