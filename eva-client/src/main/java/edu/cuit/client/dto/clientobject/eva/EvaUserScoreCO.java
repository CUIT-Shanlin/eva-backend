package edu.cuit.client.dto.clientobject.eva;


import lombok.Data;
import lombok.experimental.Accessors;
import java.util.List;

/**
 * 评教分数-一门课程
 */

@Data
@Accessors(chain = true)
public class EvaUserScoreCO {
    /**
     * 一门课程的评教分数，后端=》前端
     */
    private List<EvaLogCO> userscore;
}
